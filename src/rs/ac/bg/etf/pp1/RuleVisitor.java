package rs.ac.bg.etf.pp1;

import org.apache.log4j.*;

import rs.ac.bg.etf.pp1.ast.*;

public class RuleVisitor extends VisitorAdaptor {

	Logger log = Logger.getLogger(RuleVisitor.class);
	
	int printCallCount = 0;
	int varDeclCount = 0;
	
	public void visit(VarDecl varDecl) {
		varDeclCount++;
	}
	
	public void visit(PrintStatement printStmt) {
		printCallCount++;
		log.info("Print statement acknowledged!");
	}
	
}
