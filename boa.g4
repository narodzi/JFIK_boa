grammar boa;

prog: ( stat? NEWLINE )* 
    ;

stat:   WRTIE ID	#write
    | READINT ID    #readint
    | READREAL ID   #readreal
	| ID '=' value	#assign
   ;

value: ID
    | mathExpr
    | boolExpr
    ;	

expr0: expr1 #single0
        | expr1 ADD expr1 #add
    ;

expr1: expr2 #single1
        | expr2 SUB expr2 #sub
    ;

expr2: expr3 #single2
        | expr3 MULT expr3 #mult
    ;

expr3: mathExpr #single3
        | mathExpr DIV mathExpr #div
    ;

mathExpr: INT           #int
        | REAL          #real
        | TOINT mathExpr    #toint
        | TOREAL mathExpr   #toreal
        | '(' expr0 ')'     #par
    ;


bexpr0: bexpr1 #singleb0
        | bexpr1 AND bexpr1 #and
        | bexpr1 SCEAND bexpr1 #sceand
    ;

bexpr1: bexpr2 #singleb1
        | bexpr2 OR bexpr2 #or
        | bexpr2 SCEOR bexpr2 #sceor
    ;

bexpr2: bexpr3 #singleb2
        | bexpr3 XOR bexpr3 #xor
    ;

bexpr3: boolExpr #singleb3
        | NEG boolExpr #neg
    ;

boolExpr: BOOLEAN          #boolean
        | '(' bexpr0 ')' #bpar
    ;

READINT:	'readint' 
   ;

READREAL:	'readreal' 
   ;

WRTIE:	'write' 
    ;

TOINT: '(int)'
    ;

TOREAL: '(real)'
    ;

BOOLEAN: 'true' | 'false'
    ;

AND: 'and'
    ;

OR: 'or'
    ;

XOR: 'xor'
    ;

NEG: 'neg'
    ;

ID:   ('a'..'z'|'A'..'Z')+
   ;

REAL: [0-9]+.[0-9]+
    ;

INT:   [0-9]+
    ;

ADD: '+'
    ;

SUB: '-'
    ;

MULT: '*'
    ;

DIV: '/'
    ;

SCEAND: '&'
    ;

SCEOR: '|'
    ;

NEWLINE: '\r'? '\n'
    ;

WS: (' '|'\t')+ { skip(); }
    ;