package rs.ac.bg.etf.pp1;

import java.util.Stack;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

public class SemanticPass extends VisitorAdaptor {

	Logger log = Logger.getLogger(RuleVisitor.class);

	int printCallCount = 0;
	int varDeclCount = 0;
	String currentMethod = null;
	boolean isReturnFound = false;
	boolean isErrorDetected = false;

	public static final Struct boolType = new Struct(Struct.Bool);
	private Struct currentType = Tab.noType;
	private Struct methodType = Tab.noType;
	
	private Stack<Boolean> isNotCombined = new Stack<>(); 
	private Obj currentFactor = null;

	int nVars = 0;

	public SemanticPass() {
		Tab.init();
		Tab.currentScope().addToLocals(new Obj(Obj.Type, new String("bool"), boolType));
	}

	public void report_error(String message, SyntaxNode info) {
		isErrorDetected = true;
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0 : info.getLine();
		if (line != 0) {
			msg.append(" on line ").append(line);
		}
		log.error(msg.toString());
	}

	public void report_info(String message, SyntaxNode info) {
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0 : info.getLine();
		if (line != 0) {
			msg.append(" on line ").append(line);
		}
		log.info(msg.toString());
	}

	public void visit(ProgramName programName) {
		programName.obj = Tab.insert(Obj.Prog, programName.getProgramName(), Tab.noType);
		Tab.openScope();
		report_info("Program name : " + programName.getProgramName() + " declared", programName);
	}

	public void visit(Program program) {
		nVars = Tab.currentScope.getnVars();
		Tab.chainLocalSymbols(program.getProgramName().obj);
		Tab.closeScope();
		report_info("Program : " + program.getProgramName().getProgramName() + " declared", program);
	}

	public void visit(Type type) {
		Obj typeNode = Tab.find(type.getTypeName());
		Struct typeStruct;
		String errorMessage = null;
		if (typeNode == Tab.noObj) {
			errorMessage = " not found in symbol table";
			typeStruct = Tab.noType;
		} else {
			typeStruct = Obj.Type == typeNode.getKind() ? typeNode.getType() : Tab.noType;
			errorMessage = Obj.Type == typeNode.getKind() ? null : " is not a type";
		}
		if (errorMessage != null) {
			report_error("Error : " + type.getTypeName() + errorMessage, type);
		} else {
			report_info("Type : " + type.getTypeName() + " declared", type);
		}
		type.struct = currentType = typeStruct;
	}

	public void visit(ConstDecl constDecl) {
		Obj constValue = constDecl.getConstValue().obj;
		String errorMessage = null;
		if (constValue.getType().equals(currentType)) {
			if (Tab.currentScope.findSymbol(constDecl.getConstantName()) == null) {
				Obj newConstant = Tab.insert(constValue.getKind(), constDecl.getConstantName(), constValue.getType());
				newConstant.setAdr(constValue.getAdr());
			} else {
				errorMessage = " is already declared";
			}
		} else {
			errorMessage = " is of the wrong type";
		}
		if (errorMessage != null) {
			report_error("Error : " + constDecl.getConstantName() + errorMessage, constDecl);
		} else {
			// report_info("Const decl : " + constDecl.getConstantName() + " declared",
			// constDecl);
		}
	}

	public void visit(NumberConstant numberConstant) {
		numberConstant.obj = new Obj(Obj.Con, "", Tab.intType);
		numberConstant.obj.setAdr(numberConstant.getNumConst());
		report_info("Number constant : " + numberConstant.getNumConst() + " declared", numberConstant);
	}

	public void visit(CharConstant charConstant) {
		charConstant.obj = new Obj(Obj.Con, "", Tab.charType);
		charConstant.obj.setAdr(charConstant.getCharConst());
		report_info("Char constant : " + charConstant.getCharConst() + " declared", charConstant);
	}

	public void visit(BooleanConstant boolConstant) {
		boolConstant.obj = new Obj(Obj.Con, "", boolType);
		boolConstant.obj.setAdr(Boolean.valueOf(boolConstant.getBoolConst()) ? 1 : 0);
		report_info("Boolean constant : " + boolConstant.getBoolConst() + " declared", boolConstant);
	}

	public void visit(VarDeclaration varDeclaration) {
		if (Tab.currentScope.findSymbol(varDeclaration.getVarName()) == null) {
			Tab.insert(Obj.Var, varDeclaration.getVarName(), currentType);
			report_info("Var declaration : " + varDeclaration.getVarName() + " declared", varDeclaration);
		} else {
			report_error("Error : " + varDeclaration.getVarName() + " is already declared", varDeclaration);
		}
	}

	public void visit(VarArrayDeclaration varArrayVisit) {
		if (Tab.currentScope.findSymbol(varArrayVisit.getVarName()) == null) {
			Tab.insert(Obj.Var, varArrayVisit.getVarName(), new Struct(Struct.Array, currentType));
			report_info("Var declaration : " + varArrayVisit.getVarName() + " declared", varArrayVisit);
		} else {
			report_error("Error : " + varArrayVisit.getVarName() + " is already declared", varArrayVisit);
		}
	}

	public void visit(MethodDeclaration methodDeclaration) {
		if (methodType != Tab.noType && !isReturnFound) {
			report_error("Error : " + currentMethod + " must have a return statement", methodDeclaration);
		}
		report_info("Method declaration : " + currentMethod + " declared", methodDeclaration);
		isReturnFound = false;
		currentMethod = null;
		Obj returnTypeNameNode = methodDeclaration.getMethodInitialization().getReturnTypeName().obj;
		returnTypeNameNode.setLevel(0);
		Tab.chainLocalSymbols(returnTypeNameNode);
		Tab.closeScope();
	}

	public void visit(VoidReturn voidReturn) {
		report_info("Void return declaration for method : " + voidReturn.getMethodName() + " declared", voidReturn);
		methodType = Tab.noType;
		voidReturn.obj = Tab.insert(Obj.Meth, voidReturn.getMethodName(), methodType);
		currentMethod = voidReturn.getMethodName();
		Tab.openScope();
	}

	public void visit(NonVoidReturn nonVoidReturn) {
		report_info("Non void return declaration for method : " + nonVoidReturn.getMethodName() + " declared",
				nonVoidReturn);
		methodType = nonVoidReturn.getType().struct;
		nonVoidReturn.obj = Tab.insert(Obj.Meth, nonVoidReturn.getMethodName(), methodType);
		currentMethod = nonVoidReturn.getMethodName();
		Tab.openScope();
	}

	public void visit(ReadStatement readStatement) {
		Obj designatorNode = readStatement.getDesignator().obj;
		if (designatorNode.getType() != Tab.intType && designatorNode.getType() != Tab.charType
				&& designatorNode.getType() != boolType) {
			report_error("Error : read statement needs to be of type int, char or bool", readStatement);
		} else if (designatorNode.getKind() != Obj.Var && designatorNode.getKind() != Obj.Elem) {
			report_error("Error : only vars and elems allowed", readStatement);
		} else {
			report_info("Read statement declaration", readStatement);
		}
	}

	public void visit(PrintStatement printStatement) {
		Struct expressionNode = printStatement.getExpr().struct;
		if (expressionNode != Tab.intType && expressionNode != Tab.charType && expressionNode != boolType) {
			report_error("Error : print statement needs to be of type int, char or bool", printStatement);
		} else {
			report_info("Print statement declaration", printStatement);
		}
	}

	public void visit(PrintStatementWithNumber printStatementWithNumber) {
		Struct printExpression = printStatementWithNumber.getExpr().struct;
		if (printExpression != Tab.intType && printExpression != Tab.charType && printExpression != boolType) {
			report_error("Error : print statement needs to be of type int, char or bool", printStatementWithNumber);
		} else {
			report_info("Print statement with number declaration", printStatementWithNumber);
		}
	}

	public void visit(ReturnEmptyStatement returnEmptyStatement) {
		isReturnFound = true;
		if (methodType != Tab.noType) {
			report_error("Error : method must have a return expression with type : " + methodType.getKind(),
					returnEmptyStatement);
		} else if (currentMethod == null) {
			report_error("Error : return not allowed when not in method", returnEmptyStatement);
		} else {
			report_info("Return empty statement declaration", returnEmptyStatement);
		}
	}

	public void visit(ReturnExprStatement returnExprStatement) {
		isReturnFound = true;
		Struct returnExpression = returnExprStatement.getExpr().struct;
		if (methodType == Tab.noType || !returnExpression.assignableTo(methodType)) {
			report_error("Error : method must have a return expression with type : " + methodType.getKind(),
					returnExprStatement);
		} else if (currentMethod == null) {
			report_error("Error : return not allowed when not in method", returnExprStatement);
		} else {
			report_info("Return expr statement declaration", returnExprStatement);
		}
	}

//	public void visit(AssignDesignatorStatement assignDesignatorStatement) {
//		report_info("Assign designator statement declaration", assignDesignatorStatement);
//		Obj designatorNode = assignDesignatorStatement.getDesignator().obj;
//		Struct expressionNode = assignDesignatorStatement.getExpr().struct;
//		if (designatorNode.getKind() != Obj.Elem && designatorNode.getKind() != Obj.Var) {
//			report_error("Error : designator needs to be an elem or var", assignDesignatorStatement);
//		}
//		if (!expressionNode.assignableTo(designatorNode.getType())) {
//			report_error("Error : types mismatch", assignDesignatorStatement);
//		}
//	}

	public void visit(AssignmentStatement assignmentStatement) {
		Obj obj = assignmentStatement.getDesignator().obj;
		if (!assignmentStatement.getExpr().struct.assignableTo(obj.getType())) {
			report_error("Error : types mismatch", assignmentStatement);
		}
		if (obj.getKind() != Obj.Var && obj.getKind() != Obj.Elem) {
			report_error("Error : " + obj.getName() + "  is not an elem nor var",
					assignmentStatement);
		}
	}

	public void visit(IncrementDesignatorStatement incrementDesignatorStatement) {
		Obj designatorNode = incrementDesignatorStatement.getDesignator().obj;
		if (!designatorNode.getType().equals(Tab.intType)) {
			report_error("Error : " + designatorNode.getName() + " needs to be of type int",
					incrementDesignatorStatement);
		}
		if (designatorNode.getKind() != Obj.Var && designatorNode.getKind() != Obj.Elem) {
			report_error("Error : " + designatorNode.getName() + "  is not an elem nor var",
					incrementDesignatorStatement);
		}
		report_info("Increment designator statement declaration", incrementDesignatorStatement);
	}

	public void visit(DecrementDesignatorStatement decrementDesignatorStatement) {
		Obj designatorNode = decrementDesignatorStatement.getDesignator().obj;
		if (!designatorNode.getType().equals(Tab.intType)) {
			report_error("Error : " + designatorNode.getName() + " needs to be of type int",
					decrementDesignatorStatement);
		}
		if (designatorNode.getKind() != Obj.Var && designatorNode.getKind() != Obj.Elem) {
			report_error("Error : " + designatorNode.getName() + "  is not an elem nor var",
					decrementDesignatorStatement);
		}
		report_info("Decrement designator statement declaration", decrementDesignatorStatement);
	}

	public void visit(Expr expression) {
		expression.struct = expression.getTermList().struct;
		isNotCombined.pop();
	}
	
	public void visit(DummyExprFlag dummyExprFlag) {
		isNotCombined.push(false);
	}

	public void visit(SingleTerm singleTerm) {
		singleTerm.struct = singleTerm.getTerm().struct;
		report_info("Single term declaration", singleTerm);
	}

	public void visit(NegativeSingleTerm singleTerm) {
		if (!singleTerm.getTerm().struct.equals(Tab.intType)) {
			report_error("Error : add op terms need to be of type int", singleTerm);
			singleTerm.struct = Tab.noType;
		} else {
			singleTerm.struct = Tab.intType;
		}
		report_info("Single term declaration", singleTerm);
	}

	public void visit(MultipleTerms multipleTerms) {
		Struct firstFactor = multipleTerms.getTerm().struct;
		Struct secondFactor = multipleTerms.getTermList().struct;
		if (!firstFactor.equals(Tab.intType) || !secondFactor.equals(Tab.intType)) {
			report_error("Error : add op terms need to be of type int", multipleTerms);
			multipleTerms.struct = Tab.noType;
		} else {
			multipleTerms.struct = Tab.intType;
		}
		report_info("Multiple terms found", multipleTerms);
	}

	public void visit(SingleFactor singleFactor) {
		singleFactor.struct = singleFactor.getFactor().struct;
		report_info("Single factor found ", singleFactor);
	}

	public void visit(MultipleFactors multipleFactors) {
		Struct firstFactor = multipleFactors.getTerm().struct;
		Struct secondFactor = multipleFactors.getFactor().struct;
		if (!firstFactor.equals(Tab.intType) || !secondFactor.equals(Tab.intType)) {
			report_error("Error : mul op factors need to be of type int", multipleFactors);
		}
		report_info("Multiple factors found", multipleFactors);
		multipleFactors.struct = multipleFactors.getTerm().struct;
	}

	public void visit(NonArrayDesignator nonArrayDesignator) {
		Obj designatorNode = Tab.find(nonArrayDesignator.getDesignatorName());
		Obj designatorObj = designatorNode != Tab.noObj ? designatorNode : Tab.noObj;
		if (designatorNode != Tab.noObj) {
			report_info("Non array designator : " + nonArrayDesignator.getDesignatorName() + " found",
					nonArrayDesignator);
		} else {
			report_error("Error : " + nonArrayDesignator.getDesignatorName() + " not found", nonArrayDesignator);
		}
		nonArrayDesignator.obj = designatorObj;
	}

	public void visit(ArrayDesignator arrayDesignator) {
		Obj designatorNode = arrayDesignator.getDesignatorHelp().getDesignator().obj;
		String errorMessage = null;
		if (designatorNode.getType().getKind() != Struct.Array) {
			errorMessage = " isn't an array";
		}
		if (errorMessage != null) {
			report_error("Error : " + designatorNode.getName() + errorMessage,
					arrayDesignator);
		} else {
			report_info("Array designator : " + designatorNode.getName() + " found",
					arrayDesignator);
		}
		arrayDesignator.obj = errorMessage != null ? Tab.noObj
				: new Obj(Obj.Elem, "", designatorNode.getType().getElemType());
	}

	public void visit(NumberFactor numberFactor) {
		currentFactor = null;
		numberFactor.struct = Tab.intType;
		report_info("Number factor : " + numberFactor.getFactorValue() + " declared", numberFactor);
	}

	public void visit(CharacterFactor characterFactor) {
		currentFactor = null;
		characterFactor.struct = Tab.charType;
		report_info("Character factor : " + characterFactor.getFactorValue() + " declared", characterFactor);
	}

	public void visit(BooleanFactor booleanFactor) {
		currentFactor = null;
		booleanFactor.struct = boolType;
		report_info("Boolean factor : " + booleanFactor.getFactorValue() + " declared", booleanFactor);
	}

	public void visit(ExpressionFactor expressionFactor) {
		currentFactor = null;
		expressionFactor.struct = expressionFactor.getExpr().struct;
	}

	public void visit(DesignatorFactor designatorFactor) {
		Obj designatorNode = designatorFactor.getDesignator().obj;
		if (designatorNode.getKind() != Obj.Var && designatorNode.getKind() != Obj.Elem
				&& designatorNode.getKind() != Obj.Con) {
			report_error("Error : " + designatorFactor.getDesignator().obj.getName()
					+ " is not a variable nor an array element", designatorFactor);
		}
		report_info("Designator factor found", designatorFactor);
		designatorFactor.struct = designatorNode.getType();
		currentFactor = designatorNode;
	}

	public void visit(FuncCallFactor funcCallFactor) {
		currentFactor = null;
		Obj funcName = funcCallFactor.getDesignator().obj;
		if (funcName.getKind() != Obj.Meth) {
			report_error("Error : " + funcCallFactor.getDesignator().obj.getName()
					+ " is not a variable nor an array element", funcCallFactor);
		}
		report_info("Func call factor found", funcCallFactor);
		funcCallFactor.struct = funcName.getType();
	}

	public void visit(NewDesignatorArrayFactor newDesignatorArrayFactor) {
		currentFactor = null;
		if (newDesignatorArrayFactor.getExpr().struct != Tab.intType) {
			newDesignatorArrayFactor.struct = Tab.noType;
			report_error("Error : array size must be of type int", newDesignatorArrayFactor);
		}
		report_info("New designator array factor found", newDesignatorArrayFactor);
		newDesignatorArrayFactor.struct = new Struct(Struct.Array, newDesignatorArrayFactor.getType().struct);
	}

	public boolean passed() {
		return !isErrorDetected;
	}
	
	public void visit(MulLeftOp mulLeftOp) {
		isNotCombined.pop();
		isNotCombined.push(true);
	}
	
	public void visit(AddLeftOp addLeftOp) {
		isNotCombined.pop();
		isNotCombined.push(true);
	}
	
	public void visit(MulRightOp mulRightOp) {
		if(isNotCombined.peek()) {
			report_error("Left side of assignment can't be an expression", mulRightOp);
			isNotCombined.pop();
			isNotCombined.push(false);
		}
		if(currentFactor == null || (currentFactor.getKind() != Obj.Elem && currentFactor.getKind() != Obj.Var)) {
			report_error("Left side of assignment must be Lvalue", mulRightOp);
		}
	}
	
	public void visit(AddRightOp addRightOp) {
		if(isNotCombined.peek()) {
			report_error("Left side of assignment can't be an expression", addRightOp);
			isNotCombined.pop();
			isNotCombined.push(false);
		}
		if(currentFactor == null || (currentFactor.getKind() != Obj.Elem && currentFactor.getKind() != Obj.Var)) {
			report_error("Left side of assignment must be Lvalue", addRightOp);
		}
	}

}
