package org.mdmi.core.engine.xml;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.mdmi.core.engine.xml.XPathParser.AxisSpecifierContext;
import org.mdmi.core.engine.xml.XPathParser.NodeTestContext;
import org.mdmi.core.engine.xml.XPathParser.PredicateContext;
import org.mdmi.core.engine.xml.XPathParser.StepContext;

public class TestDemo {
	public static void main(String[] args) throws Exception {

		String expression = "eid[@mdmiType='docEid']";
		XPathLexer lexer = new XPathLexer(new ANTLRInputStream(expression));
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		XPathParser parser = new XPathParser(tokens);

		ParseTreeWalker walker = new ParseTreeWalker(); // create standard walker

		final StringBuffer sb = new StringBuffer();

		// System.out.println(expression);
		ParseTreeListener extractor = new XPathBaseListener() {

			boolean inPredicate = false;

			boolean isAttribute = false;

			@Override
			public void enterNodeTest(NodeTestContext ctx) {
				if (!inPredicate) {
					// // System.out.println(" aaa " +ctx.getText());
				}
				super.enterNodeTest(ctx);
			}

			@Override
			public void exitNodeTest(NodeTestContext ctx) {
				if (!inPredicate) {
					// // System.out.println(" bbb " +ctx.getText());
					if (isAttribute) {
						sb.append(ctx.getText());
					} else {
						sb.append(ctx.getText()).append("/");
					}
					isAttribute = false;
				}
				super.exitNodeTest(ctx);
			}

			@Override
			public void enterPredicate(PredicateContext ctx) {
				inPredicate = true;
				// // System.out.println(" enterPredicate " +ctx.getText());
				super.enterPredicate(ctx);
			}

			@Override
			public void exitPredicate(PredicateContext ctx) {
				inPredicate = false;
				// // System.out.println(" enterPredicate " +ctx.getText());
				super.exitPredicate(ctx);
			}

			@Override
			public void enterAxisSpecifier(AxisSpecifierContext ctx) {
				// // System.out.println(" enterAxisSpecifier " +ctx.getText());
				super.enterAxisSpecifier(ctx);
			}

			@Override
			public void exitAxisSpecifier(AxisSpecifierContext ctx) {
				// // System.out.println(" exitAxisSpecifier " +ctx.getText());
				if (!inPredicate && "@".equals(ctx.getText())) {
					isAttribute = true;
					sb.append("@");
				}
				super.exitAxisSpecifier(ctx);
			}

			// @Override
			// public void enterAxisSpecifier( AxisSpecifierContext ctx ) {
			// skip = true;
			// super.enterAxisSpecifier(ctx);
			// }
			//
			// @Override
			// public void exitAxisSpecifier( AxisSpecifierContext ctx ) {
			// skip = false;
			// super.exitAxisSpecifier(ctx);
			// }

			// @Override
			// public void exitNodeTest( NodeTestContext ctx ) {
			// // System.out.println(" exitNodeTest " +ctx.getText());
			// super.exitNodeTest(ctx);
			// }
			//
			@Override
			public void enterStep(StepContext ctx) {
				// // System.out.println(" Begin Step" +ctx.getText());
				super.enterStep(ctx);
			}

			@Override
			public void exitStep(StepContext ctx) {
				// // System.out.println(" Exit Step" +ctx.getText());
				super.exitStep(ctx);
			}
			//
			// @Override
			// public void enterNodeTest( NodeTestContext ctx ) {
			//// // System.out.println(ctx.getText());
			// super.enterNodeTest(ctx);
			// }
			//
			// @Override
			// public void enterQName( QNameContext ctx ) {
			// // System.out.println("QQQQ"+ctx.getText());
			// super.enterQName(ctx);
			// }
			//
			// @Override
			// public void enterExpr( ExprContext ctx ) {
			//// // System.out.println("EEEEEEE"+ctx.getText());
			// super.enterExpr(ctx);
			// }

			// @Override
			// public void enterAbsoluteLocationPathNoroot( AbsoluteLocationPathNorootContext ctx ) {
			// // System.out.println(ctx.toString());
			// super.enterAbsoluteLocationPathNoroot(ctx);
			// }
			//
			// @Override
			// public void enterPathExprNoRoot( PathExprNoRootContext ctx ) {
			// // System.out.println(ctx.toString());
			// super.enterPathExprNoRoot(ctx);
			// }

		};
		walker.walk(extractor, parser.main()); // initiate walk of tree with listener

		// System.out.println(sb.toString());

		// String expression= "12*(5-6)";
		// XPathLexer lexer = new XPathLexer(new ANTLRInputStream(expression));
		// CommonTokenStream tokens = new CommonTokenStream(lexer);
		// XPathParser parser = new XPathParser(tokens);
		// Token t = parser.getCurrentToken();
		// parser.main();
		//
		// while(parser.getCurrentToken() != null) {
		// Token a = parser.consume();
		// }
		//
		// parser.main();
	}
}
