package game.util;

import framework.game.ICfgReader;
import framework.game.IKernel;

import java.util.Objects;

public class XML {
	private final String xml;
	private final XML next;
	private final Parser parser;

	public interface Parser {
		void parse(IKernel kernel, ICfgReader cfg);
	}

	public XML(String xml, XML next, IKernel kernel, Parser parser) {
		this.xml = Objects.requireNonNull(xml, "arg xml can not be null");
		this.next = next;
		this.parser = Objects.requireNonNull(parser, "arg parser can not be null");
		ICfgReader cfg = kernel.loadXmlConfig(this.xml);
		if (cfg == null) {
			return;
		}
		this.parser.parse(kernel, cfg);
	}

	public void parse(IKernel kernel, String xml) {
		if (this.xml.equals(xml)) {
			ICfgReader cfg = kernel.loadXmlConfig(xml);
			if (cfg == null) {
				return;
			}
			this.parser.parse(kernel, cfg);
		} else {
			if (this.next != null) {
				this.next.parse(kernel, xml);
			}
		}
	}
}