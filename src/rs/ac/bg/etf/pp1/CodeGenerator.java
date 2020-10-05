package rs.ac.bg.etf.pp1;

import java.util.*;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

public class CodeGenerator extends VisitorAdaptor {

	private static class RightOpElement {
		int operation;
		Obj operand;
	}

	Stack<Stack<RightOpElement>> rightOpStack = new Stack<>();

	private Obj currentFactor = null;

	String message = "";
	String message2 = null;
	private int mainPc;

	public int getMainPc() {
		return mainPc;
	}

	public void setMainPc(int newMainPc) {
		mainPc = newMainPc;
	}

	public void visit(MethodDeclaration methodDeclaration) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}

	public void initializeMethod(ReturnTypeName returnTypeName) {

		Code.put(Code.enter);
		Code.put(0);
		Code.put(returnTypeName.obj.getLocalSymbols().size());
	}

	public void visit(VoidReturn voidReturn) {
		if ("main".equalsIgnoreCase(voidReturn.getMethodName())) {
			setMainPc(Code.pc);
		}
		voidReturn.obj.setAdr(Code.pc);
		initializeMethod(voidReturn);
	}

	public void visit(NonVoidReturn nonVoidReturn) {
		if ("main".equalsIgnoreCase(nonVoidReturn.getMethodName())) {
			setMainPc(Code.pc);
		}
		nonVoidReturn.obj.setAdr(Code.pc);
		initializeMethod(nonVoidReturn);
	}

	public void visit(ReadStatement readStatement) {
		Obj readStatementDesignator = readStatement.getDesignator().obj;
		int readCode = readStatementDesignator.getType().equals(Tab.charType) ? Code.bread : Code.read;
		Code.put(readCode);
		Code.store(readStatementDesignator);
	}

	public void initializePrintStatement(int printValue, int printCode) {
		Code.loadConst(printValue);
		Code.put(printCode);
	}

	public void visit(PrintStatement printStatement) {
		boolean isCharType = printStatement.getExpr().struct == Tab.charType;
		int printValue = isCharType ? 1 : 5;
		int printCode = isCharType ? Code.bprint : Code.print;
		initializePrintStatement(printValue, printCode);
	}

	public void visit(PrintStatementWithNumber printStatementWithNumber) {
		boolean isCharType = printStatementWithNumber.getExpr().struct == Tab.charType;
		int printCode = isCharType ? Code.bprint : Code.print;
		initializePrintStatement(printStatementWithNumber.getN2(), printCode);
	}

	public void visit(AssignmentStatement assignmentStatement) {
		if (assignmentStatement.getAssignop() instanceof AssignAddopRight) {
			AssignAddopRight operation = (AssignAddopRight) assignmentStatement.getAssignop();
			if (operation.getAddopRight() instanceof PlusEqualOp) {
				Code.put(Code.add);
			} else {
				Code.put(Code.sub);
			}
		} else if (assignmentStatement.getAssignop() instanceof AssignMulopRight) {
			AssignMulopRight operation = (AssignMulopRight) assignmentStatement.getAssignop();
			if (operation.getMulopRight() instanceof TimesEqualOp) {
				Code.put(Code.mul);
			} else if (operation.getMulopRight() instanceof DivEqualOp) {
				Code.put(Code.div);
			} else {
				Code.put(Code.rem);
			}
		}
		Code.store(assignmentStatement.getDesignator().obj);
	}

	public void visit(IncrementDesignatorStatement incrementDesignatorStatement) {
		if (incrementDesignatorStatement.getDesignator().getClass() == ArrayDesignator.class) {
			Code.put(Code.dup2);
		}
		Code.load(incrementDesignatorStatement.getDesignator().obj);
		Code.loadConst(1);
		Code.put(Code.add);
		Code.store(incrementDesignatorStatement.getDesignator().obj);
	}

	public void visit(DecrementDesignatorStatement decrementDesignatorStatement) {
		if (decrementDesignatorStatement.getDesignator().getClass() == ArrayDesignator.class) {
			Code.put(Code.dup2);
		}
		Code.load(decrementDesignatorStatement.getDesignator().obj);
		Code.loadConst(1);
		Code.put(Code.sub);
		Code.store(decrementDesignatorStatement.getDesignator().obj);
	}

	public void visit(NegativeSingleTerm negativeSingleTerm) {
		if (currentFactor != null) {
			Code.load(currentFactor);
			currentFactor = null;
		}
		Code.put(Code.neg);
	}

	public void visit(MultipleTerms multipleTerms) {
		if (multipleTerms.getAddop() instanceof AddLeftOp) {
			if (currentFactor != null) {
				Code.load(currentFactor);
				currentFactor = null;
			}
			AddLeftOp addOp = (AddLeftOp) multipleTerms.getAddop();
			if (addOp.getAddopLeft() instanceof PlusOp) {
				Code.put(Code.add);
			} else {
				Code.put(Code.sub);
			}
		}
	}

	public void visit(MultipleFactors multipleFactors) {
		if (multipleFactors.getMulop() instanceof MulLeftOp) {
			if (currentFactor != null) {
				Code.load(currentFactor);
				currentFactor = null;
			}
			MulLeftOp mulOp = (MulLeftOp) multipleFactors.getMulop();
			if (mulOp.getMulopLeft() instanceof TimesOp) {
				Code.put(Code.mul);
			} else if (mulOp.getMulopLeft() instanceof DivOp) {
				Code.put(Code.div);
			} else {
				Code.put(Code.rem);
			}
		}
	}

	public void visit(NumberFactor numberFactor) {
		currentFactor = null;
		Code.loadConst(numberFactor.getFactorValue());
	}

	public void visit(CharacterFactor characterFactor) {
		currentFactor = null;
		Code.loadConst(characterFactor.getFactorValue());
	}

	public void visit(BooleanFactor booleanFactor) {
		currentFactor = null;
		int adr = Boolean.valueOf(booleanFactor.getFactorValue()) ? 1 : 0;
		Code.loadConst(adr);
	}

	public void visit(ExpressionFactor expressionFactor) {
		currentFactor = null;
	}

	public void visit(DesignatorFactor designatorFactor) {
		currentFactor = designatorFactor.getDesignator().obj;
	}

	public void visit(FuncCallFactor funcCallFactor) {
		currentFactor = null;
		Obj factor = funcCallFactor.getDesignator().obj;
		Code.put(Code.call);
		Code.put2(factor.getAdr() - Code.pc);
	}

	public void visit(NewDesignatorArrayFactor newDesignatorArrayFactor) {
		currentFactor = null;
		Code.put(Code.newarray);
		Code.put(newDesignatorArrayFactor.struct.getElemType().equals(Tab.charType) ? 0 : 1);
	}

	public void visit(DesignatorHelp designatorHelp) {
		Code.load(designatorHelp.getDesignator().obj);
	}

	public void visit(AssignAddopRight assignAddopRight) {
		AssignmentStatement assignStatement = (AssignmentStatement) assignAddopRight.getParent();
		if (assignStatement.getDesignator().getClass() == ArrayDesignator.class) {
			Code.put(Code.dup2);
		}
		Code.load(assignStatement.getDesignator().obj);
	}

	public void visit(AssignMulopRight assignMulopRight) {
		AssignmentStatement assignmentStatement = (AssignmentStatement) assignMulopRight.getParent();
		if (assignmentStatement.getDesignator().getClass() == ArrayDesignator.class) {
			Code.put(Code.dup2);
		}
		Code.load(assignmentStatement.getDesignator().obj);
	}

	public void visit(DummyExprFlag dummyExprFlag) {
		rightOpStack.push(new Stack<>());
	}

	public void visit(Expr expr) {
		if (currentFactor != null) {
			Code.load(currentFactor);
			currentFactor = null;
		}
		Stack<RightOpElement> currentRightOpStack = rightOpStack.pop();
		while (!currentRightOpStack.empty()) {
			RightOpElement element = currentRightOpStack.pop();
			Code.put(element.operation);
			if (element.operand.getKind() == Obj.Elem) {
				Code.put(Code.dup_x2);
			} else {
				Code.put(Code.dup);
			}
			Code.store(element.operand);
		}
	}

	public void visit(AddLeftOp addLeftOp) {
		if (currentFactor != null) {
			Code.load(currentFactor);
			currentFactor = null;
		}
		
		// a^b = 3+a*b*b - b+a
		
		// c = a*b + d ^ e
		
		// niz@2 = niz[2] + niz[niz.length-2-1];
		
		// ['A'#2] = 'C'
		// [['A'#2] # -3]= 'Z'

//		Code.put(Code.enter);
//		Code.put(2);
//		Code.put(3);
//		
//		Code.put(Code.load_n);
//		Code.put(Code.store_2);
//		Code.put(Code.load_1);		
//		
//		Code.put(Code.exit);
	}

	public void visit(AddRightOp addRightOp) {
		RightOpElement element = new RightOpElement();
		element.operand = currentFactor;
		if (addRightOp.getAddopRight() instanceof PlusEqualOp) {
			element.operation = Code.add;
		} else {
			element.operation = Code.sub;
		}
		rightOpStack.peek().push(element);
		if (currentFactor.getKind() == Obj.Elem) {
			Code.put(Code.dup2);
		}
		Code.load(currentFactor);
		currentFactor = null;
	}

	public void visit(MulLeftOp mulLeftOp) {
		if (currentFactor != null) {
			Code.load(currentFactor);
			currentFactor = null;
		}
	}

	public void visit(MulRightOp mulRightOp) {
		RightOpElement element = new RightOpElement();
		element.operand = currentFactor;
		if (mulRightOp.getMulopRight() instanceof TimesEqualOp) {
			element.operation = Code.mul;
		} else if (mulRightOp.getMulopRight() instanceof DivEqualOp) {
			element.operation = Code.div;
		} else {
			element.operation = Code.rem;
		}
		rightOpStack.peek().push(element);
		if (currentFactor.getKind() == Obj.Elem) {
			Code.put(Code.dup2);
		}
		Code.load(currentFactor);
		currentFactor = null;
	}

//	public void visit(AddTwoArrays addTwoArrays) {
//		if (currentFactor != null) {
//			Code.load(currentFactor);
//			currentFactor = null;
//		}
//		
//		Code.put(Code.enter);
//		Code.put(2);
//		Code.put(4);
//		Code.put(Code.const_n);
//		Code.put(Code.store_2);
//		
//		Code.put(Code.load_n);
//		Code.put(Code.arraylength);
//		Code.put(Code.newarray);
//		Code.put(Code.const_1);
//		Code.put(Code.store_3);
//
//		int whileStart = Code.pc;
//		Code.put(Code.load_2);
//		Code.put(Code.load_n);
//		Code.put(Code.arraylength);
//		
//		Code.putFalseJump(Code.lt, 0);
//		int whileSkip = Code.pc - 2;
//		
//		Code.put(Code.load_3);
//		Code.put(Code.load_2);
//		
//		Code.put(Code.load_n);
//		Code.put(Code.load_2);
//		Code.put(Code.aload);
//		
//		Code.put(Code.load_1);
//		Code.put(Code.load_2);
//		Code.put(Code.aload);
//		
//		Code.put(Code.add);
//		Code.put(Code.astore);
//		
//		Code.put(Code.load_2);
//		Code.put(Code.const_1);
//		Code.put(Code.add);
//		Code.put(Code.store_2);
//
//		Code.putJump(whileStart);
//		Code.fixup(whileSkip);
//		Code.put(Code.load_3);
//		
//		Code.put(Code.exit);
//	}
//	
//	public void visit(NonArrayDesignator DesignatorSingle) {
//		if(DesignatorSingle.getParent().getClass() == AddTwoArrays.class) {
//			Code.load(DesignatorSingle.obj);
//		}
//	}
	
//	public void visit(AtFactor atFactor) {
//		if (currentFactor != null) {
//			Code.load(currentFactor);
//			currentFactor = null;
//		}
//		Code.put(Code.enter);
//		Code.put(2);
//		Code.put(4);
//		
//		Code.put(Code.load_n);
//		Code.put(Code.load_1);
//		Code.put(Code.aload);
//		
//		Code.put(Code.load_n);
//		Code.put(Code.load_n);
//		Code.put(Code.arraylength);
//		Code.loadConst(1);
//		Code.put(Code.sub);
//		Code.put(Code.load_1);
//		Code.put(Code.sub);
//		Code.put(Code.aload);
//		
//		Code.put(Code.add);
//		Code.put(Code.exit);
//		
//	}
//	
//	public void visit(NonArrayDesignator nonArrayDesignator) {
//		if(nonArrayDesignator.getParent().getClass() == AtFactor.class) {
//			Code.load(nonArrayDesignator.obj);
//		}
//	}
//	
//	public void visit(HashFactor hashFactor) {
//		Code.put(Code.enter);
//		Code.put(2);
//		Code.put(3);
//		
//		Code.put(Code.load_n);
//		Code.put(Code.load_1);
//		Code.loadConst(26);
//		Code.put(Code.rem);
//		Code.put(Code.add);
//		Code.put(Code.store_2);
//		//if
//		Code.put(Code.load_2);
//		Code.loadConst('A');
//		Code.putFalseJump(Code.lt, 0);
//		int actualFutureAddr = Code.pc - 2;
//		
//		Code.put(Code.load_2);
//		Code.loadConst(26);
//		Code.put(Code.add);
//		Code.putJump(0);
//		
//		//else
//		int actualElseJump = Code.pc - 2;
//		
//		Code.fixup(actualFutureAddr);
//		Code.put(Code.load_2);
//		
//		Code.fixup(actualElseJump);
//		
//		//if
//		Code.put(Code.store_2);
//		
//		Code.put(Code.load_2);
//		Code.loadConst('Z');
//		Code.putFalseJump(Code.gt, 0);
//		actualFutureAddr = Code.pc - 2;
//		
//		Code.put(Code.load_2);
//		Code.loadConst(26);
//		Code.put(Code.sub);
//		Code.put(Code.store_2);
//		
//		Code.fixup(actualFutureAddr);
//		
//		
//		Code.put(Code.load_2);
//		
//		Code.put(Code.exit);
//	}
	
//	public void visit(ModifList singleModifList) {
//		Code.put(Code.enter);
//		Code.put(2);
//		Code.put(3);
//		
//		Code.put(Code.load_n);
//		Code.put(Code.load_1);
//		Code.loadConst(26);
//		Code.put(Code.rem);
//		Code.put(Code.add);
//		Code.put(Code.store_2);
//		//if
//		Code.put(Code.load_2);
//		Code.loadConst('A');
//		Code.putFalseJump(Code.lt, 0);
//		int actualFutureAddr = Code.pc - 2;
//		
//		Code.put(Code.load_2);
//		Code.loadConst(26);
//		Code.put(Code.add);
//		Code.putJump(0);
//		
//		//else
//		int actualElseJump = Code.pc - 2;
//		
//		Code.fixup(actualFutureAddr);
//		Code.put(Code.load_2);
//		
//		Code.fixup(actualElseJump);
//		
//		//if
//		Code.put(Code.store_2);
//		
//		Code.put(Code.load_2);
//		Code.loadConst('Z');
//		Code.putFalseJump(Code.gt, 0);
//		actualFutureAddr = Code.pc - 2;
//		
//		Code.put(Code.load_2);
//		Code.loadConst(26);
//		Code.put(Code.sub);
//		Code.put(Code.store_2);
//		
//		Code.fixup(actualFutureAddr);
//		
//		
//		Code.put(Code.load_2);
//		
//		Code.put(Code.exit);
//	}
	
}
