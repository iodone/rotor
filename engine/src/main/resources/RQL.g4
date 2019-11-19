
grammar RQL;

@header {
package xmatrix.dsl.parser;
}

//load jdbc.`mysql1.tb_v_user` as mysql_tb_user;
//save csv_input_result as json.`/tmp/todd/result_json` partitionBy uid;
statement
    : (sql ender)*
    ;

sql
    : ('load'|'LOAD') format '.' path ('options'|'where')? expression? booleanExpression* 'as' tableName
    | ('select'|'SELECT') ~(';')*  'as' tableName ('options'|'where')? expression? booleanExpression*
    | ('connect'|'CONNECT') format ('options'|'where')? expression? booleanExpression* ('as' db)?
    |  SIMPLE_COMMENT
    ;

booleanExpression
    : 'and' expression
    ;

expression
    : identifier '=' STRING
    ;

ender
    :';'
    ;

format
    : identifier
    ;

path
    : quotedIdentifier | identifier
    ;

setValue
    : qualifiedName | quotedIdentifier | STRING
    ;

setKey
    : qualifiedName
    ;

db
    :qualifiedName | identifier
    ;

tableName
    : identifier
    ;

functionName
    : identifier
    ;

col
    : identifier
    ;

qualifiedName
    : identifier ('.' identifier)*
    ;

identifier
    : strictIdentifier
    ;

strictIdentifier
    : IDENTIFIER
    | quotedIdentifier
    ;

quotedIdentifier
    : BACKQUOTED_IDENTIFIER
    ;


STRING
    : '\'' ( ~('\''|'\\') | ('\\' .) )* '\''
    | '"' ( ~('"'|'\\') | ('\\' .) )* '"'
    ;

IDENTIFIER
    : (LETTER | DIGIT | '_')+
    ;

BACKQUOTED_IDENTIFIER
    : '`' ( ~'`' | '``' )* '`'
    ;

fragment DIGIT
    : [0-9]
    ;

fragment LETTER
    : [a-zA-Z]
    ;

SIMPLE_COMMENT
    : '--' ~[\r\n]* '\r'? '\n'? -> channel(HIDDEN)
    ;

BRACKETED_EMPTY_COMMENT
    : '/**/' -> channel(HIDDEN)
    ;

BRACKETED_COMMENT
    : '/*' ~[+] .*? '*/' -> channel(HIDDEN)
    ;

WS
    : [ \r\n\t]+ -> channel(HIDDEN)
    ;

// Catch-all for anything we can't recognize.
// We use this to be able to ignore and recover all the text
// when splitting statements with DelimiterLexer
UNRECOGNIZED
    : .
    ;
