# Google Play 金币购买前端对接文档

本文档对应新的 `ShopModule.java` Google Play 消耗型商品链路。该链路是全新的，不走旧商城、旧预下单、旧支付回调、旧 `MallsModule` 发货逻辑。

前端不要调用旧协议：

- `C2S_GET_STORE_ITEMS = 30`
- `C2S_BUY_STORE_PAY = 9`
- `CustomMsg.PreOrder`
- `S2C_PREORDER_RESP = 20`
- `REQ_ORDER_RES = 29`
- 旧 `payHost`、`payBack` 或第三方支付回调地址

## 1. 接口总览

三个接口都走项目现有请求封装：

```text
外层请求：CLIENT_REQUEST = 5
请求体：ClientMsg.RequestMsg(msgid, reqid, data)
data：CustomMsg.String，value 为 JSON 字符串

外层响应：CLIENT_RESPONSE = 6
响应体：ClientMsg.ResponseMsg
响应 data：CustomMsg.String，value 为 JSON 字符串
```

| 接口 | msgid | 用途 |
|---|---:|---|
| 获取商品白名单 | `10020` | 获取服务端允许售卖的 Google Product ID 和奖励 |
| 购买前检查 | `10019` | 检查资格，获取 BillingFlowParams 用的 `obfuscatedAccountId` |
| 验单发货 | `10018` | 提交 `purchaseToken`，服务端验单、发货、保存、consume |

`reqid` 需要在当前连接未完成请求中唯一。收到响应后按 `ResponseMsg.reqid` 找回调。

## 2. 完整流程

```text
登录游戏
  -> 请求 10020 获取服务端 Google 商品白名单
  -> BillingClient.queryProductDetailsAsync(INAPP)
  -> 前端只展示“服务端白名单和 Google ProductDetails 都存在”的商品
  -> 用户点击购买
  -> 请求 10019 PREPARE
  -> 使用返回的 obfuscatedAccountId 拉起 launchBillingFlow
  -> 收到 PurchaseState.PURCHASED
  -> 请求 10018，提交 productId + purchaseToken
  -> 服务端调用 Google Android Publisher API 验单
  -> 服务端向 PlayerMoney 发金币并强制保存玩家数据
  -> 服务端标记订单 SHIPPED
  -> 服务端 consume Google 商品
  -> code=0，前端确认发货终态，金币显示跟随服务端 PlayerMoney 绝对余额同步
```

客户端不要调用 `consumeAsync()` 或 `acknowledgePurchase()`。本项目由服务端在奖励落库后执行 consume。

## 3. 10020 获取商品列表

请求：

```json
{}
```

成功响应：

```json
{
  "code": 0,
  "status": "ok",
  "products": [
    {
      "productId": "google_gold_12",
      "rewardVersion": 2,
      "balanceProperty": "PlayerMoney",
      "gold": 600000,
      "headId": 0,
      "headHours": 0
    }
  ]
}
```

当前配置：

| Product ID | 金币 | 默认状态 |
|---|---:|---|
| `google_gold_1` | 1,000 | 关闭，仅用于沙盒联调 |
| `google_gold_12` | 600,000 | 开启 |
| `google_gold_28` | 1,400,000 | 开启 |
| `google_gold_50` | 2,500,000 | 开启 |
| `google_gold_108` | 5,400,000 | 开启 |
| `google_gold_328` | 16,400,000 | 开启 |
| `google_gold_648` | 32,400,000 | 开启 |

服务端不下发展示价格。商品标题、描述、币种、格式化价格必须使用 Google `ProductDetails` 返回的数据。

本次 P5 钱包切换（2026-10-27）统一使用游戏权威字段 `PlayerMoney`。部署代码及 XML 后，新订单快照使用 `rewardVersion=2`；六档商品与关闭的沙盒档版本均为 2，奖励数量及开关不变。10020 和购买成功响应的 `balanceProperty` 固定为 `PlayerMoney`，不再按订单版本选择旧 `Gold` 钱包。

历史 `rewardVersion=1` 已发货订单保留原快照，重试只返回 `already_fulfilled`，不迁移历史 `Gold`、不回写订单版本、不向 `PlayerMoney` 补发。未执行的货币发放步骤统一使用 `PlayerMoney`，已有 `REWARD_STEP` 仍阻止重复发放。

## 4. 查询 Google ProductDetails

前端只展示 `10020` 白名单和 Google 查询结果的交集。

```kotlin
val queryProducts = serverProducts.map {
    QueryProductDetailsParams.Product.newBuilder()
        .setProductId(it.productId)
        .setProductType(BillingClient.ProductType.INAPP)
        .build()
}

val params = QueryProductDetailsParams.newBuilder()
    .setProductList(queryProducts)
    .build()

billingClient.queryProductDetailsAsync(params) { result, queryResult ->
    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
        val details = queryResult.productDetailsList
        val unfetched = queryResult.unfetchedProductList
        // UI 只展示 details 中同时存在于服务端白名单的商品
    }
}
```

不要长期缓存 `ProductDetails`。用户购买前应使用当前 Billing 连接查到的对象。

## 5. 10019 购买前检查

用户每次点击购买都要先请求 PREPARE。

请求：

```json
{
  "productId": "google_gold_12"
}
```

成功响应：

```json
{
  "code": 0,
  "status": "ready",
  "eligible": true,
  "productId": "google_gold_12",
  "obfuscatedAccountId": "64位小写十六进制字符串"
}
```

前端必须把 `obfuscatedAccountId` 原样传给 `BillingFlowParams.setObfuscatedAccountId()`。它是服务端 HMAC 结果，不包含明文 UID，客户端不要自行生成、修改或跨账号复用。

常见失败：

| code | status | 前端处理 |
|---:|---|---|
| `6` | `invalid_request` | 参数错误，不拉起 Billing |
| `8` | `server_not_configured` / `invalid_player` / `server_error` | 显示暂不可用并上报 |
| `14` | `product_not_found` | 刷新 10020 商品列表 |
| `23` | `real_name_required` | 引导实名 |
| `23` | `age_restricted` | 展示年龄限制 |
| `23` | `minor_payment_limit` | 展示单笔或月度限额 |

PREPARE 只决定能不能拉起本次支付。已经从 Google 获得的合法 `purchaseToken`，仍必须提交 10018 验单，不能因为商品随后下架就丢弃 token。

## 6. 拉起 Billing

Billing 8 初始化示例：

```kotlin
val billingClient = BillingClient.newBuilder(context)
    .setListener(purchasesUpdatedListener)
    .enablePendingPurchases(
        PendingPurchasesParams.newBuilder()
            .enableOneTimeProducts()
            .build()
    )
    .enableAutoServiceReconnection()
    .build()
```

购买示例：

```kotlin
val offer = productDetails.oneTimePurchaseOfferDetailsList.firstOrNull()
    ?: return showProductUnavailable()

val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
    .setProductDetails(productDetails)
    .setOfferToken(offer.offerToken)
    .build()

val flowParams = BillingFlowParams.newBuilder()
    .setProductDetailsParamsList(listOf(productParams))
    .setObfuscatedAccountId(prepare.obfuscatedAccountId)
    .build()

val result = billingClient.launchBillingFlow(activity, flowParams)
```

一次只提交一个商品。服务端只接受 `quantity == 1`。

## 7. 处理 Purchase

```kotlin
private fun handlePurchase(purchase: Purchase) {
    when (purchase.purchaseState) {
        Purchase.PurchaseState.PENDING -> showPending()
        Purchase.PurchaseState.PURCHASED -> {
            val productId = purchase.products.singleOrNull()
                ?: return reportInvalidPurchase()
            if (productId !in serverProductIds) return reportInvalidPurchase()
            submitBuy(productId, purchase.purchaseToken)
        }
    }
}
```

处理规则：

- 用户取消：结束支付态，不请求 10018。
- `PENDING`：不发货，稍后通过 `queryPurchasesAsync(INAPP)` 恢复。
- `PURCHASED`：请求 10018。
- 不要把完整 `purchaseToken` 写入日志、埋点、剪贴板或崩溃平台。

## 8. 10018 验单发货

请求只能包含：

```json
{
  "productId": "google_gold_12",
  "purchaseToken": "Purchase.purchaseToken 原值"
}
```

不要上传价格、UID、包名、奖励数量、订单成功标记或 `obfuscatedAccountId`。

首次成功响应：

```json
{
  "code": 0,
  "status": "fulfilled",
  "productId": "google_gold_12",
  "orderId": "GPA.1234-5678-9012-34567",
  "duplicate": false,
  "consumePending": false,
  "rewardVersion": 2,
  "balanceProperty": "PlayerMoney",
  "rewardGold": 600000,
  "gold": 123456789,
  "headId": 0
}
```

同一 token 重试成功响应：

```json
{
  "code": 0,
  "status": "already_fulfilled",
  "productId": "google_gold_12",
  "orderId": "GPA.1234-5678-9012-34567",
  "duplicate": true,
  "consumePending": false,
  "rewardVersion": 2,
  "balanceProperty": "PlayerMoney",
  "rewardGold": 600000,
  "gold": 123456789,
  "headId": 0
}
```

只有 `code == 0` 表示奖励已经持久化。`consumePending == true` 也表示已到账，只是 Google consume 待服务端后续补偿；客户端要保留同一 token，不要重新购买同一笔。

`code=23,status=processing,retryable=true` 是一次查询的中间态响应，不是发货终态，也不承诺随后向这个 reqid 再推一条终态。保留原 productId + purchaseToken 并退避重试；后续请求将返回 code=0/fulfilled 或 already_fulfilled，或明确的失败/人工审核状态。余额属性推送不替代该终态确认。

10018 的字段名 `gold` 保持兼容，值始终是当前 `PlayerMoney` 绝对余额（含历史 v1 订单的重试回包），不是本单奖励增量；`rewardGold` 才是订单快照中的奖励数量。客户端只用服务端同步的 `PlayerMoney` 覆盖余额，不得把 `gold` 或 `rewardGold` 再加到本地钱包。`rewardVersion` 在历史订单响应中仍可能为 1，不能据此把返回余额解释为旧 `Gold`。

## 9. 错误和重试

| code | status | 前端处理 |
|---:|---|---|
| `0` | `fulfilled` / `already_fulfilled` | 到账，刷新金币余额 |
| `6` | `invalid_request` / `google_product_mismatch` / `google_token_mismatch` | 不自动重试，上报协议异常 |
| `8` | `google_api_error` / `service_busy` / `delivery_db_unavailable` | `retryable=true` 时用同一 token 退避重试 |
| `8` | `player_save_pending` / `delivery_record_pending` | 同一 token 退避重试 |
| `8` | `reward_dispatch_failed` / `recharge_tracking_failed` | 保留 token，禁止重新购买，提示稍后恢复 |
| `8` | `payment_record_missing` / `payment_record_full` / `payment_record_corrupt` | 保留 token，停止高频重试并告警 |
| `8` | `lease_heartbeat_unavailable` | 同一 token 延迟重试 |
| `14` | `product_not_found` | 刷新 10020；已付款 token 必须保留并告警 |
| `22` | `pending` | 等 Google 状态变为 `PURCHASED` |
| `22` | `not_purchased` / `unsupported_quantity` | 停止自动重试，核查 Google 订单 |
| `22` | `account_mismatch` / `token_already_claimed` / `token_product_mismatch` | 停止重试并告警 |
| `22` | `fulfillment_config_mismatch` / `consumed_without_fulfillment` | 停止自动处理，人工核查 |
| `23` | `processing` / `verification_busy` / `lease_ack_stale` / `lease_lost` / `player_fulfillment_busy` | 同一 token 退避重试 |

人工审核相关：

| status | 前端处理 |
|---|---|
| `reward_state_unknown` | `manualReview=true`，停止自动重试和重新购买，提交 token 哈希、UID 给运营核账 |
| `manual_review_record_pending` | 全局人工审核标记未确认，停止自动发货重试并告警 |
| `manual_review_save_pending` | 全局已阻断自动发货，可延迟提交同一 token 查询状态，不能重新购买或客户端 consume |

普通可重试状态建议使用 `1s -> 2s -> 5s -> 10s -> 30s` 退避，之后进入本地待补单队列。所有重试必须复用原始 `productId + purchaseToken`。

严禁：

- 10018 超时后再次 `launchBillingFlow()`。
- 客户端 `consume` 或 `acknowledge`。
- 收到非零 `code` 就删除本地 token。
- 切换游戏账号后把旧账号 token 交给新账号。

## 10. 断线恢复

以下时机调用 `queryPurchasesAsync(ProductType.INAPP)`：

- Billing 连接成功。
- 游戏账号登录完成。
- App 回到前台。
- 10018 超时或返回可重试状态。

将所有 `PURCHASED` 且仍存在于 Google 商品库的订单重新提交 10018。`PENDING` 只展示状态，不请求发货。

## 11. 前端验收清单

- 10020 只展示新 Google SKU，旧商城商品不参与。
- UI 价格完全来自 Google `ProductDetails`。
- 正常购买只发一次金币，金币余额刷新正确。
- 同 token 串行、并发、断线重试都只到账一次。
- `PENDING -> PURCHASED` 之前不发货。
- 付款后杀进程，重启可通过 `queryPurchasesAsync` 补交。
- 错 SKU、错 token、`quantity > 1` 均不发货。
- A 账号购买的 token 由 B 账号提交时返回 `account_mismatch`。
- 非零 code 不删除 token，按 `retryable` 和 `status` 处理。

## 12. 服务端上线依赖

前端联调前，服务端需要完成：

- 执行 `tank-server/sql/20260715_google_play_order.sql` 创建独立订单表。
- Play Console 创建与 `tank-server/config/res/GooglePlay/GooglePlayProducts.xml` 一致的 one-time Product ID。
- 服务账号授予目标应用 Android Publisher 权限。
- 配置 `googlePlayPackageName`、`googlePlayServiceAccountPath`、`googlePlayAccountIdSecret`。
- `googlePlayAccountIdSecret` 至少 32 字符，必须稳定，不能随意轮换。

### P5 钱包切换部署顺序

1. 先发布 P5 新客户端（vc16+）；其对旧后端 `rewardVersion=1` 商品应显示暂不可用。
2. 部署含钱包切换的服务端代码和 `GooglePlayProducts.xml`，首次切换建议重启 game 进程，保证 Java 实现与配置同时生效；不需要 SQL 或余额迁移。
3. 代码生效后，后续 XML 调整支持通过管理命令 `updateCfg res/GooglePlay/GooglePlayProducts.xml` 触发 `RefreshCfg` 热加载。配置文件必须先更新到各 game 实例实际使用的配置目录；检查加载日志和 10020 实际回包，不能只看管理命令的 success（配置校验失败会保留旧商品表）。
4. 客户端重新请求 10020 或重登，确认六档 `rewardVersion=2`、`balanceProperty=PlayerMoney` 后，用新客户端做许可测试购买及升级／结算连续性验收。现有登录会话中的属性声明不变，无需强制重登才能接收余额同步。

旧客户端可能把绝对余额再次加到本地显示值；切换窗口不使用旧包真实购买。已有 v2 订单后，不应回滚到向 `Gold` 发货的实现，应关闭新购入口并向前修复，保留订单记录。

### 后台核账口径

坦克金币以玩家 `PlayerMoney` 为准，IAP 通过 `PROP_CHANGE` 记录 `google iap reward amount=...`。共享框架／旧商城仍有 `Gold` 引用（包括 GM 的 `OnTestAddValue(type=0)`、后台扣道具日志、旧商城和通用玩家日志），不能以这些旧字段核验坦克充值到账；历史 `Gold` 仅用于核对切换前旧订单。本工单保留这些旧模块及历史数据，不做跨模块钱包替换。

