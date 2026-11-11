package game.modules.weapon;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import framework.PropertyKey;
import framework.game.*;
import framework.mybatis.domain.GooglePlayOrder;
import framework.mybatis.service.impl.GooglePlayOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/** Keyset scans persist progress with refunds; only a fully covered inventory may revoke account rights. */
final class PayingEntitlementReconciler implements PropertyKey, AutoCloseable {
    interface Audit { GooglePlayPurchaseVerifier.Entitlement assess(GooglePlayOrder order, String account) throws Exception; }
    static final String PROGRESS = "GooglePlayAuditState";
    static final long DEFAULT_POLL_MS = 10000L;
    private static final Logger log = LoggerFactory.getLogger(PayingEntitlementReconciler.class);
    private static final long BATCH_MS = 30000, RESULT_MAX_AGE_MS = 90000;
    private static final int PAGE_SIZE = 8;
    public static final class Progress {
        public String stamp, localSignature;
        public long cursor, covered;
        public boolean active, unknown, paying;
        public int revision;
        public Set<String> remaining = new HashSet<>();
    }
    private static final class Page {
        String stamp;
        long count, maxId;
        List<GooglePlayOrder> orders;
    }
    private static final class Entry {
        GooglePlayOrder order;
        GooglePlayPurchaseVerifier.Entitlement result;
        long at;
    }
    private final IKernel kernel;
    private final String packageName;
    private final Audit audit;
    private final IntFunction<String> accountId;
    private final IntPredicate fulfillmentBusy;
    private final Executor io;
    private final LongSupplier now;
    private final Set<Integer> inFlight = ConcurrentHashMap.newKeySet();
    private final Set<Integer> retryScheduled = ConcurrentHashMap.newKeySet();
    private ScheduledExecutorService timer;
    private volatile boolean closed;
    private int auditsPerMinute = 60, used;
    private long window = Long.MIN_VALUE;

    PayingEntitlementReconciler(IKernel kernel, String packageName, Audit audit,
                               IntFunction<String> accountId, IntPredicate fulfillmentBusy) {
        this(kernel, packageName, audit, accountId, fulfillmentBusy,
                new ThreadPoolExecutor(2,2,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(128),task -> {
                    Thread thread=new Thread(task,"google-entitlement-io"); thread.setDaemon(true); return thread;
                },new ThreadPoolExecutor.AbortPolicy()), () -> TimeUnit.NANOSECONDS.toMillis(System.nanoTime()));
    }
    PayingEntitlementReconciler(IKernel kernel, String packageName, Audit audit, IntFunction<String> accountId,
                               IntPredicate fulfillmentBusy, Executor io, LongSupplier now) {
        this.kernel=kernel; this.packageName=packageName; this.audit=audit; this.accountId=accountId;
        this.fulfillmentBusy=fulfillmentBusy; this.io=io; this.now=now;
    }
    synchronized void setAuditsPerMinute(int limit) {
        if (limit < 1 || limit > 100000) throw new IllegalArgumentException("invalid entitlement audit budget");
        auditsPerMinute=limit;
    }
    private synchronized boolean permit() {
        long time=now.getAsLong();
        if (window==Long.MIN_VALUE || time-window>=60000) {window=time; used=0;}
        if (used>=auditsPerMinute) return false;
        used++; return true;
    }
    void start(long intervalMs) {
        start(intervalMs,Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread=new Thread(task,"google-entitlement-poll"); thread.setDaemon(true); return thread;
        }));
    }
    void start(long intervalMs,ScheduledExecutorService scheduler) {
        timer=scheduler;
        long effectiveInterval=Math.max(DEFAULT_POLL_MS,intervalMs);
        timer.scheduleWithFixedDelay(() -> kernel.runOnLogicThread(() -> {
            if (closed) return;
            List<IGameObject> players=kernel.getAllOnlinePlayer();
            if (players!=null) for (IGameObject player:players) check(player);
        }),DEFAULT_POLL_MS,effectiveInterval,TimeUnit.MILLISECONDS);
        log.info("paying entitlement polling enabled intervalMs={} pageSize={} auditsPerMinute={} offlinePolicy=check_on_next_login",
                effectiveInterval,PAGE_SIZE,auditsPerMinute);
    }
    void check(IGameObject player) {
        if (player==null || closed) return;
        int uid=player.getInt(PLAYER_PROPERTY_UID);
        if (uid<=0 || kernel.getPlayer(uid)!=player || fulfillmentBusy.test(uid) || !inFlight.add(uid)) return;
        int revision=player.getInt(PLAYER_PAYING_ENTITLEMENT_REVISION);
        boolean active=player.getBool(PLAYER_IS_PAYING);
        Progress saved=readProgress(player);
        long cursor=saved==null?0:saved.cursor;
        page(uid,cursor,first -> {
            if (first==null || !current(player,uid,revision,active)) {finish(uid,"inventory_or_session_changed",player,30000);return;}
            Progress progress=saved;
            String local=localSignature(player);
            if (progress==null || !first.stamp.equals(progress.stamp) || revision!=progress.revision
                    || active!=progress.paying || !local.equals(progress.localSignature)) {
                progress=fresh(player,first,revision,active,local);
                if (cursor!=0) {saveProgress(player,uid,revision,active,progress,"inventory_restart",500);return;}
            }
            final Progress next=progress;
            Set<String> known=new HashSet<>();
            for (GooglePlayOrder order:first.orders) if (GooglePlayRefunds.knownRefund(player,order)) known.add(order.getGoogleOrderId());
            try {
                io.execute(() -> {
                    List<Entry> entries=new ArrayList<>();
                    long started=now.getAsLong();
                    String expected;
                    try {expected=accountId.apply(uid);} catch (RuntimeException invalid) {expected=null;}
                    for (GooglePlayOrder order:first.orders) {
                        if (closed || (!entries.isEmpty() && now.getAsLong()-started>=BATCH_MS)) break;
                        Entry entry=new Entry();entry.order=order;
                        if (known.contains(order.getGoogleOrderId())) entry.result=new GooglePlayPurchaseVerifier.Entitlement(false,true);
                        else {
                            if (expected==null || expected.isEmpty() || !permit()) break;
                            try {entry.result=audit.assess(order,expected);}
                            catch (Exception failure) {
                                log.warn("paying entitlement audit unavailable uid={} orderId={} exceptionType={}",
                                        uid,order.getGoogleOrderId(),failure.getClass().getSimpleName());
                            }
                        }
                        if (entry.result==null) entry.result=new GooglePlayPurchaseVerifier.Entitlement(null,false);
                        entry.at=now.getAsLong();entries.add(entry);
                    }
                    kernel.runOnLogicThread(() -> {
                        if (!current(player,uid,revision,active)) {finish(uid,"audit_session_changed",player,30000);return;}
                        page(uid,cursor,latest -> {
                            if (latest==null || !first.stamp.equals(latest.stamp)
                                    || !signature(first.orders).equals(signature(latest.orders))
                                    || !next.localSignature.equals(localSignature(player))
                                    || !current(player,uid,revision,active)) {
                                finish(uid,"purchase_inventory_changed",player,30000);return;
                            }
                            if (entries.isEmpty() && !first.orders.isEmpty()) {finish(uid,"audit_budget_or_configuration",player,15000);return;}
                            boolean refundSave=false, freshActive=false;
                            for (Entry entry:entries) {
                                GooglePlayPurchaseVerifier.Entitlement result=entry.result;
                                boolean fresh=now.getAsLong()-entry.at<=RESULT_MAX_AGE_MS;
                                next.unknown |= !fresh || result.active==null;
                                if (fresh) {
                                    next.active |= Boolean.TRUE.equals(result.active);
                                    freshActive |= Boolean.TRUE.equals(result.active);
                                    if (result.refunded) {
                                        try {refundSave |= GooglePlayRefunds.recover(player,entry.order);}
                                        catch (RuntimeException failure) {
                                            refundSave=true;
                                            log.error("Google refund requires review uid={} orderId={} exceptionType={}",
                                                    uid,entry.order.getGoogleOrderId(),failure.getClass().getSimpleName());
                                        }
                                    }
                                }
                                next.cursor=entry.order.getId();next.covered++;
                                next.remaining.remove(entry.order.getGoogleOrderId());
                                log.info("paying entitlement order assessed uid={} orderId={} active={} refunded={} cursor={}",
                                        uid,entry.order.getGoogleOrderId(),result.active,result.refunded,next.cursor);
                            }
                            boolean complete=next.cursor>=first.maxId;
                            if (complete && next.covered!=first.count) next.unknown=true;
                            // Positive ownership needs only one fresh valid order. Persist the new pair with progress.
                            if (!complete && !active && freshActive && revision>=0 && revision<Integer.MAX_VALUE) {
                                next.revision=revision+1;next.paying=true;
                                player.setProperty(PROGRESS,JSON.toJSONString(next));
                                apply(player,uid,revision,false,true,500);return;
                            }
                            if (!complete) {saveProgress(player,uid,revision,active,next,"batch_saved",500);return;}
                            Boolean decision=next.active?(active || freshActive?Boolean.TRUE:null)
                                    :(next.unknown || !next.remaining.isEmpty()?null:Boolean.FALSE);
                            if (Boolean.FALSE.equals(decision) && (player.getBool(PLAYER_DOUBLE_COINS_PURCHASED)
                                    || (first.count==0 && active))) decision=null;
                            final Boolean finalDecision=decision;
                            if (!refundSave && saved==null) {
                                if(finalDecision==null) finish(uid,"entitlement_uncertain",player,30000);
                                else apply(player,uid,revision,active,finalDecision);
                                return;
                            }
                            player.setProperty(PROGRESS,"");
                            persist(player,uid,revision,active,savedOk -> {
                                if (!savedOk) {finish(uid,"refund_or_progress_save_pending",player,30000);return;}
                                // Recheck inventory after the asynchronous refund archive ACK, too.
                                page(uid,0,confirmed -> {
                                    if (confirmed==null || !first.stamp.equals(confirmed.stamp)
                                            || !next.localSignature.equals(localSignature(player)) || !current(player,uid,revision,active)) {
                                        finish(uid,"purchase_inventory_changed_after_save",player,30000);return;
                                    }
                                    if (finalDecision==null) finish(uid,"entitlement_uncertain",player,30000);
                                    else apply(player,uid,revision,active,finalDecision);
                                });
                            });
                        });
                    });
                });
            } catch (RejectedExecutionException rejected) {finish(uid,"audit_queue_busy",player,30000);}
        });
    }
    private Progress fresh(IGameObject player,Page page,int revision,boolean paying,String local) {
        Progress result=new Progress();result.stamp=page.stamp;result.localSignature=local;result.revision=revision;result.paying=paying;
        IRecord rec=player.getRecord("GooglePlayOrderRec");
        if (rec!=null) for(int row=0;row<rec.getRows();row++) if(rec.getInt(row,1)==3 || rec.getInt(row,1)==4) {
            result.remaining.add(rec.getString(row,3));result.unknown |= rec.getInt(row,15)==1;
        }
        return result;
    }
    private String localSignature(IGameObject player) {
        List<String> identities=new ArrayList<>();IRecord rec=player.getRecord("GooglePlayOrderRec");
        if(rec!=null) for(int row=0;row<rec.getRows();row++) if(rec.getInt(row,1)==3 || rec.getInt(row,1)==4)
            identities.add(rec.getString(row,3)+"|"+rec.getInt(row,15));
        Collections.sort(identities);return identities.toString();
    }
    private Progress readProgress(IGameObject player) {
        try {
            Progress result=JSON.parseObject(player.getString(PROGRESS),Progress.class);
            return result!=null && result.cursor>=0 && result.covered>=0 && result.remaining!=null?result:null;
        } catch(RuntimeException invalid) {return null;}
    }
    private void saveProgress(IGameObject player,int uid,int revision,boolean active,Progress progress,String reason,long delay) {
        player.setProperty(PROGRESS,JSON.toJSONString(progress));
        persist(player,uid,revision,active,saved -> finish(uid,saved?reason:"progress_save_pending",player,saved?delay:30000));
    }
    private void persist(IGameObject player,int uid,int revision,boolean active,Consumer<Boolean> callback) {
        try {kernel.flushGooglePlayPlayer(player,saved -> {
            log.info("Google refund archive saved uid={} saved={} pendingReview={}",uid,Boolean.TRUE.equals(saved),GooglePlayRefunds.pendingCount(player));
            callback.accept(Boolean.TRUE.equals(saved) && current(player,uid,revision,active));
        });} catch(RuntimeException failure) {callback.accept(false);}
    }
    private void page(int uid,long after,Consumer<Page> callback) {
        kernel.executeGooglePlayToStore(GooglePlayOrderService.class,"fulfilledPageForUid",Arrays.asList(uid,packageName,after),data -> {
            Page page=null;
            try {
                JSONObject json=JSON.parseObject(data);
                if(json!=null) {
                    page=new Page();page.count=json.getLongValue("orderCount");page.maxId=json.getLongValue("maxId");
                    if(!json.containsKey("orderCount") || !json.containsKey("maxId") || json.getString("version")==null
                            || page.count<0 || page.maxId<0) throw new IllegalArgumentException();
                    page.stamp=page.count+"|"+page.maxId+"|"+json.getString("version");
                    page.orders=json.getJSONArray("orders").toJavaList(GooglePlayOrder.class);
                    if(page.orders.size()>PAGE_SIZE) throw new IllegalArgumentException();
                    Set<String> identities=new HashSet<>();long previous=after;
                    for(GooglePlayOrder order:page.orders) {
                        if(order==null || order.getId()==null || order.getId()<=previous || order.getId()>page.maxId
                                || !Integer.valueOf(uid).equals(order.getUid()) || !packageName.equals(order.getPackageName())
                                || order.getProductId()==null || order.getGoogleOrderId()==null || order.getGoogleOrderId().isEmpty()
                                || order.getPurchaseTokenHash()==null || order.getStatus()==null
                                || (order.getStatus()!=3 && order.getStatus()!=4) || !identities.add(order.getGoogleOrderId()))
                            throw new IllegalArgumentException();
                        previous=order.getId();
                    }
                    if(page.orders.isEmpty() && after<page.maxId) throw new IllegalArgumentException();
                }
            } catch(RuntimeException invalid) {page=null;}
            callback.accept(page);
        });
    }
    private boolean current(IGameObject player,int uid,int revision,boolean active) {
        return !closed && kernel.getPlayer(uid)==player && !fulfillmentBusy.test(uid)
                && player.getInt(PLAYER_PAYING_ENTITLEMENT_REVISION)==revision && player.getBool(PLAYER_IS_PAYING)==active;
    }
    private void apply(IGameObject player,int uid,int revision,boolean oldActive,boolean active) {
        apply(player,uid,revision,oldActive,active,0);
    }
    private void apply(IGameObject player,int uid,int revision,boolean oldActive,boolean active,long continuation) {
        if(!active && player.getBool(PLAYER_DOUBLE_COINS_PURCHASED)) {finish(uid,"legacy_ownership_requires_review",player,0);return;}
        if(revision<0 || (revision==Integer.MAX_VALUE && oldActive!=active)) {finish(uid,"invalid_revision",player,0);return;}
        try {
            if(revision==0 || oldActive!=active) {
                if(revision==Integer.MAX_VALUE) {finish(uid,"revision_exhausted",player,0);return;}
                player.setProperty(PLAYER_IS_PAYING,active,IKernel.PlayerLogType.PROP_CHANGE.ordinal(),"official purchase entitlement reconciliation");
                player.setProperty(PLAYER_PAYING_ENTITLEMENT_REVISION,revision+1);
                if(player.getBool(PLAYER_IS_PAYING)!=active || player.getInt(PLAYER_PAYING_ENTITLEMENT_REVISION)!=revision+1) throw new IllegalStateException();
            }
        } catch(RuntimeException failure) {
            try {player.setProperty(PLAYER_IS_PAYING,oldActive);player.setProperty(PLAYER_PAYING_ENTITLEMENT_REVISION,revision);} catch(RuntimeException ignored) { }
            finish(uid,"property_update_pending",player,30000);return;
        }
        try {kernel.flushGooglePlayPlayer(player,saved -> {
            log.info("paying entitlement reconciled uid={} active={} revision={} saved={}",uid,active,player.getInt(PLAYER_PAYING_ENTITLEMENT_REVISION),Boolean.TRUE.equals(saved));
            finish(uid,Boolean.TRUE.equals(saved)?"saved":"save_pending",player,Boolean.TRUE.equals(saved)?continuation:30000);
        });} catch(RuntimeException failure) {finish(uid,"save_pending",player,30000);}
    }
    private static String signature(List<GooglePlayOrder> orders) {
        List<String> parts=new ArrayList<>();
        for(GooglePlayOrder order:orders) parts.add(order.getId()+"|"+order.getGoogleOrderId()+"|"+order.getProductId()+"|"
                +order.getPurchaseTokenHash()+"|"+order.getStatus()+"|"+order.getGoldAmount()+"|"+order.getQuantity()+"|"
                +order.getFulfillmentHash()+"|"+order.getRewardVersion());
        return parts.toString();
    }
    private void finish(int uid,String reason,IGameObject player,long delay) {
        inFlight.remove(uid);
        if(!"saved".equals(reason)) log.info("paying entitlement check deferred uid={} reason={}",uid,reason);
        if(delay>0 && !closed && timer!=null && retryScheduled.add(uid)) {
            try {timer.schedule(() -> kernel.runOnLogicThread(() -> {
                retryScheduled.remove(uid);
                if(kernel.getPlayer(uid)==player) check(player);
            }),delay,TimeUnit.MILLISECONDS);}
            catch(RejectedExecutionException stopped) {retryScheduled.remove(uid);}
        }
    }
    @Override public void close() {
        closed=true;if(timer!=null)timer.shutdownNow();if(io instanceof ExecutorService)((ExecutorService)io).shutdownNow();inFlight.clear();retryScheduled.clear();
    }
}
