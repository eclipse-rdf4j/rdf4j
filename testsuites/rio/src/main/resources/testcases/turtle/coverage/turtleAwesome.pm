####################################################################
#
#    This file was generated using Parse::Yapp version 1.05.
#
#        Don't edit this file, use source file instead.
#
#             ANY CHANGE MADE HERE WILL BE LOST !
#
####################################################################
package turtleAwesome;
use vars qw ( @ISA );
use strict;

@ISA= qw ( Parse::Yapp::Driver );
#Included Parse/Yapp/Driver.pm file----------------------------------------
{
#
# Module Parse::Yapp::Driver
#
# This module is part of the Parse::Yapp package available on your
# nearest CPAN
#
# Any use of this module in a standalone parser make the included
# text under the same copyright as the Parse::Yapp module itself.
#
# This notice should remain unchanged.
#
# (c) Copyright 1998-2001 Francois Desarmenien, all rights reserved.
# (see the pod text in Parse::Yapp module for use and distribution rights)
#

package Parse::Yapp::Driver;

require 5.004;

use strict;

use vars qw ( $VERSION $COMPATIBLE $FILENAME );

$VERSION = '1.05';
$COMPATIBLE = '0.07';
$FILENAME=__FILE__;

use Carp;

#Known parameters, all starting with YY (leading YY will be discarded)
my(%params)=(YYLEX => 'CODE', 'YYERROR' => 'CODE', YYVERSION => '',
			 YYRULES => 'ARRAY', YYSTATES => 'ARRAY', YYDEBUG => '');
#Mandatory parameters
my(@params)=('LEX','RULES','STATES');

sub new {
    my($class)=shift;
	my($errst,$nberr,$token,$value,$check,$dotpos);
    my($self)={ ERROR => \&_Error,
				ERRST => \$errst,
                NBERR => \$nberr,
				TOKEN => \$token,
				VALUE => \$value,
				DOTPOS => \$dotpos,
				STACK => [],
				DEBUG => 0,
				CHECK => \$check };

	_CheckParams( [], \%params, \@_, $self );

		exists($$self{VERSION})
	and	$$self{VERSION} < $COMPATIBLE
	and	croak "Yapp driver version $VERSION ".
			  "incompatible with version $$self{VERSION}:\n".
			  "Please recompile parser module.";

        ref($class)
    and $class=ref($class);

    bless($self,$class);
}

sub YYParse {
    my($self)=shift;
    my($retval);

	_CheckParams( \@params, \%params, \@_, $self );

	if($$self{DEBUG}) {
		_DBLoad();
		$retval = eval '$self->_DBParse()';#Do not create stab entry on compile
        $@ and die $@;
	}
	else {
		$retval = $self->_Parse();
	}
    $retval
}

sub YYData {
	my($self)=shift;

		exists($$self{USER})
	or	$$self{USER}={};

	$$self{USER};
	
}

sub YYErrok {
	my($self)=shift;

	${$$self{ERRST}}=0;
    undef;
}

sub YYNberr {
	my($self)=shift;

	${$$self{NBERR}};
}

sub YYRecovering {
	my($self)=shift;

	${$$self{ERRST}} != 0;
}

sub YYAbort {
	my($self)=shift;

	${$$self{CHECK}}='ABORT';
    undef;
}

sub YYAccept {
	my($self)=shift;

	${$$self{CHECK}}='ACCEPT';
    undef;
}

sub YYError {
	my($self)=shift;

	${$$self{CHECK}}='ERROR';
    undef;
}

sub YYSemval {
	my($self)=shift;
	my($index)= $_[0] - ${$$self{DOTPOS}} - 1;

		$index < 0
	and	-$index <= @{$$self{STACK}}
	and	return $$self{STACK}[$index][1];

	undef;	#Invalid index
}

sub YYCurtok {
	my($self)=shift;

        @_
    and ${$$self{TOKEN}}=$_[0];
    ${$$self{TOKEN}};
}

sub YYCurval {
	my($self)=shift;

        @_
    and ${$$self{VALUE}}=$_[0];
    ${$$self{VALUE}};
}

sub YYExpect {
    my($self)=shift;

    keys %{$self->{STATES}[$self->{STACK}[-1][0]]{ACTIONS}}
}

sub YYLexer {
    my($self)=shift;

	$$self{LEX};
}


#################
# Private stuff #
#################


sub _CheckParams {
	my($mandatory,$checklist,$inarray,$outhash)=@_;
	my($prm,$value);
	my($prmlst)={};

	while(($prm,$value)=splice(@$inarray,0,2)) {
        $prm=uc($prm);
			exists($$checklist{$prm})
		or	croak("Unknow parameter '$prm'");
			ref($value) eq $$checklist{$prm}
		or	croak("Invalid value for parameter '$prm'");
        $prm=unpack('@2A*',$prm);
		$$outhash{$prm}=$value;
	}
	for (@$mandatory) {
			exists($$outhash{$_})
		or	croak("Missing mandatory parameter '".lc($_)."'");
	}
}

sub _Error {
	print "Parse error.\n";
}

sub _DBLoad {
	{
		no strict 'refs';

			exists(${__PACKAGE__.'::'}{_DBParse})#Already loaded ?
		and	return;
	}
	my($fname)=__FILE__;
	my(@drv);
	open(DRV,"<$fname") or die "Report this as a BUG: Cannot open $fname";
	while(<DRV>) {
                	/^\s*sub\s+_Parse\s*{\s*$/ .. /^\s*}\s*#\s*_Parse\s*$/
        	and     do {
                	s/^#DBG>//;
                	push(@drv,$_);
        	}
	}
	close(DRV);

	$drv[0]=~s/_P/_DBP/;
	eval join('',@drv);
}

#Note that for loading debugging version of the driver,
#this file will be parsed from 'sub _Parse' up to '}#_Parse' inclusive.
#So, DO NOT remove comment at end of sub !!!
sub _Parse {
    my($self)=shift;

	my($rules,$states,$lex,$error)
     = @$self{ 'RULES', 'STATES', 'LEX', 'ERROR' };
	my($errstatus,$nberror,$token,$value,$stack,$check,$dotpos)
     = @$self{ 'ERRST', 'NBERR', 'TOKEN', 'VALUE', 'STACK', 'CHECK', 'DOTPOS' };

#DBG>	my($debug)=$$self{DEBUG};
#DBG>	my($dbgerror)=0;

#DBG>	my($ShowCurToken) = sub {
#DBG>		my($tok)='>';
#DBG>		for (split('',$$token)) {
#DBG>			$tok.=		(ord($_) < 32 or ord($_) > 126)
#DBG>					?	sprintf('<%02X>',ord($_))
#DBG>					:	$_;
#DBG>		}
#DBG>		$tok.='<';
#DBG>	};

	$$errstatus=0;
	$$nberror=0;
	($$token,$$value)=(undef,undef);
	@$stack=( [ 0, undef ] );
	$$check='';

    while(1) {
        my($actions,$act,$stateno);

        $stateno=$$stack[-1][0];
        $actions=$$states[$stateno];

#DBG>	print STDERR ('-' x 40),"\n";
#DBG>		$debug & 0x2
#DBG>	and	print STDERR "In state $stateno:\n";
#DBG>		$debug & 0x08
#DBG>	and	print STDERR "Stack:[".
#DBG>					 join(',',map { $$_[0] } @$stack).
#DBG>					 "]\n";


        if  (exists($$actions{ACTIONS})) {

				defined($$token)
            or	do {
				($$token,$$value)=&$lex($self);
#DBG>				$debug & 0x01
#DBG>			and	print STDERR "Need token. Got ".&$ShowCurToken."\n";
			};

            $act=   exists($$actions{ACTIONS}{$$token})
                    ?   $$actions{ACTIONS}{$$token}
                    :   exists($$actions{DEFAULT})
                        ?   $$actions{DEFAULT}
                        :   undef;
        }
        else {
            $act=$$actions{DEFAULT};
#DBG>			$debug & 0x01
#DBG>		and	print STDERR "Don't need token.\n";
        }

            defined($act)
        and do {

                $act > 0
            and do {        #shift

#DBG>				$debug & 0x04
#DBG>			and	print STDERR "Shift and go to state $act.\n";

					$$errstatus
				and	do {
					--$$errstatus;

#DBG>					$debug & 0x10
#DBG>				and	$dbgerror
#DBG>				and	$$errstatus == 0
#DBG>				and	do {
#DBG>					print STDERR "**End of Error recovery.\n";
#DBG>					$dbgerror=0;
#DBG>				};
				};


                push(@$stack,[ $act, $$value ]);

					$$token ne ''	#Don't eat the eof
				and	$$token=$$value=undef;
                next;
            };

            #reduce
            my($lhs,$len,$code,@sempar,$semval);
            ($lhs,$len,$code)=@{$$rules[-$act]};

#DBG>			$debug & 0x04
#DBG>		and	$act
#DBG>		and	print STDERR "Reduce using rule ".-$act." ($lhs,$len): ";

                $act
            or  $self->YYAccept();

            $$dotpos=$len;

                unpack('A1',$lhs) eq '@'    #In line rule
            and do {
                    $lhs =~ /^\@[0-9]+\-([0-9]+)$/
                or  die "In line rule name '$lhs' ill formed: ".
                        "report it as a BUG.\n";
                $$dotpos = $1;
            };

            @sempar =       $$dotpos
                        ?   map { $$_[1] } @$stack[ -$$dotpos .. -1 ]
                        :   ();

            $semval = $code ? &$code( $self, @sempar )
                            : @sempar ? $sempar[0] : undef;

            splice(@$stack,-$len,$len);

                $$check eq 'ACCEPT'
            and do {

#DBG>			$debug & 0x04
#DBG>		and	print STDERR "Accept.\n";

				return($semval);
			};

                $$check eq 'ABORT'
            and	do {

#DBG>			$debug & 0x04
#DBG>		and	print STDERR "Abort.\n";

				return(undef);

			};

#DBG>			$debug & 0x04
#DBG>		and	print STDERR "Back to state $$stack[-1][0], then ";

                $$check eq 'ERROR'
            or  do {
#DBG>				$debug & 0x04
#DBG>			and	print STDERR 
#DBG>				    "go to state $$states[$$stack[-1][0]]{GOTOS}{$lhs}.\n";

#DBG>				$debug & 0x10
#DBG>			and	$dbgerror
#DBG>			and	$$errstatus == 0
#DBG>			and	do {
#DBG>				print STDERR "**End of Error recovery.\n";
#DBG>				$dbgerror=0;
#DBG>			};

			    push(@$stack,
                     [ $$states[$$stack[-1][0]]{GOTOS}{$lhs}, $semval ]);
                $$check='';
                next;
            };

#DBG>			$debug & 0x04
#DBG>		and	print STDERR "Forced Error recovery.\n";

            $$check='';

        };

        #Error
            $$errstatus
        or   do {

            $$errstatus = 1;
            &$error($self);
                $$errstatus # if 0, then YYErrok has been called
            or  next;       # so continue parsing

#DBG>			$debug & 0x10
#DBG>		and	do {
#DBG>			print STDERR "**Entering Error recovery.\n";
#DBG>			++$dbgerror;
#DBG>		};

            ++$$nberror;

        };

			$$errstatus == 3	#The next token is not valid: discard it
		and	do {
				$$token eq ''	# End of input: no hope
			and	do {
#DBG>				$debug & 0x10
#DBG>			and	print STDERR "**At eof: aborting.\n";
				return(undef);
			};

#DBG>			$debug & 0x10
#DBG>		and	print STDERR "**Dicard invalid token ".&$ShowCurToken.".\n";

			$$token=$$value=undef;
		};

        $$errstatus=3;

		while(	  @$stack
			  and (		not exists($$states[$$stack[-1][0]]{ACTIONS})
			        or  not exists($$states[$$stack[-1][0]]{ACTIONS}{error})
					or	$$states[$$stack[-1][0]]{ACTIONS}{error} <= 0)) {

#DBG>			$debug & 0x10
#DBG>		and	print STDERR "**Pop state $$stack[-1][0].\n";

			pop(@$stack);
		}

			@$stack
		or	do {

#DBG>			$debug & 0x10
#DBG>		and	print STDERR "**No state left on stack: aborting.\n";

			return(undef);
		};

		#shift the error token

#DBG>			$debug & 0x10
#DBG>		and	print STDERR "**Shift \$error token and go to state ".
#DBG>						 $$states[$$stack[-1][0]]{ACTIONS}{error}.
#DBG>						 ".\n";

		push(@$stack, [ $$states[$$stack[-1][0]]{ACTIONS}{error}, undef ]);

    }

    #never reached
	croak("Error in driver logic. Please, report it as a BUG");

}#_Parse
#DO NOT remove comment

1;

}
#End of include--------------------------------------------------


#line 1 "turtleAwesome.yp"

# START TokenBlock
my $GT_DOT = "\\.";
my $GT_SEMI = ";";
my $GT_COMMA = ",";
my $GT_LBRACKET = "\\[";
my $GT_RBRACKET = "\\]";
my $GT_LPAREN = "\\(";
my $GT_RPAREN = "\\)";
my $GT_DTYPE = "\\^\\^";
my $IT_true = "true";
my $IT_false = "false";
my $SPARQL_PREFIX = "[Pp][Rr][Ee][Ff][Ii][Xx]";
my $SPARQL_BASE = "[Bb][Aa][Ss][Ee]";
my $BASE = "\@[Bb][Aa][Ss][Ee]";
my $PREFIX = "\@[Pp][Rr][Ee][Ff][Ii][Xx]";
my $RDF_TYPE = "a";
my $LANGTAG = "\@(?:[A-Za-z])+(?:(?:-(?:[0-9A-Za-z])+))*";
my $INTEGER = "(?:[\\+-])?(?:[0-9])+";
my $DECIMAL = "(?:[\\+-])?(?:[0-9])*\\.(?:[0-9])+";
my $EXPONENT = "[Ee](?:[\\+-])?(?:[0-9])+";
my $DOUBLE = "(?:[\\+-])?(?:(?:(?:[0-9])+\\.(?:[0-9])*(?:${EXPONENT}))|(?:(?:\\.)?(?:[0-9])+(?:${EXPONENT})))";
my $ECHAR = "\\\\[\\\"\\'\\\\bfnrt]";
my $WS = "(?: )|(?:(?:\\t)|(?:(?:\\r)|(?:\\n)))";
my $ANON = "\\[(?:(?:${WS}))*\\]";
my $PN_CHARS_BASE = "(?:[A-Z])|(?:(?:[a-z])|(?:(?:[\x{00C0}-\x{00D6}])|(?:(?:[\x{00D8}-\x{00F6}])|(?:(?:[\x{00F8}-\x{02FF}])|(?:(?:[\x{0370}-\x{037D}])|(?:(?:[\x{037F}-\x{1FFF}])|(?:(?:[\x{200C}-\x{200D}])|(?:(?:[\x{2070}-\x{218F}])|(?:(?:[\x{2C00}-\x{2FEF}])|(?:(?:[\x{3001}-\x{D7FF}])|(?:(?:[\x{F900}-\x{FDCF}])|(?:(?:[\x{FDF0}-\x{FFFD}])|(?:[\x{10000}-\x{EFFFF}])))))))))))))";
my $PN_CHARS_U = "(?:(?:${PN_CHARS_BASE}))|(?:_)";
my $PN_CHARS = "(?:(?:${PN_CHARS_U}))|(?:(?:-)|(?:(?:[0-9])|(?:(?:[\x{00B7}])|(?:(?:[\x{0300}-\x{036F}])|(?:[\x{203F}-\x{2040}])))))";
my $BLANK_NODE_LABEL = "_:(?:(?:(?:${PN_CHARS_U}))|(?:[0-9]))(?:(?:(?:(?:(?:(?:${PN_CHARS}))|(?:\\.)))*(?:${PN_CHARS})))?";
my $PN_PREFIX = "(?:${PN_CHARS_BASE})(?:(?:(?:(?:(?:(?:${PN_CHARS}))|(?:\\.)))*(?:${PN_CHARS})))?";
my $PNAME_NS = "(?:(?:${PN_PREFIX}))?:";
my $HEX = "(?:[0-9])|(?:(?:[A-F])|(?:[a-f]))";
my $PERCENT = "\%(?:${HEX})(?:${HEX})";
my $UCHAR = "(?:\\\\u(?:${HEX})(?:${HEX})(?:${HEX})(?:${HEX}))|(?:\\\\U(?:${HEX})(?:${HEX})(?:${HEX})(?:${HEX})(?:${HEX})(?:${HEX})(?:${HEX})(?:${HEX}))";
my $STRING_LITERAL1 = "\\'(?:(?:(?:[\x{0000}-\\t\x{000B}-\x{000C}\x{000E}-&\\(-\\[\\]-\x{10FFFD}])|(?:(?:(?:${ECHAR}))|(?:(?:${UCHAR})))))*\\'";
my $STRING_LITERAL2 = "\\\"(?:(?:(?:[\x{0000}-\\t\x{000B}-\x{000C}\x{000E}-!#-\\[\\]-\x{10FFFD}])|(?:(?:(?:${ECHAR}))|(?:(?:${UCHAR})))))*\\\"";
my $STRING_LITERAL_LONG1 = "\\'\\'\\'(?:(?:(?:(?:(?:\\')|(?:\\'\\')))?(?:(?:[\x{0000}-&\\(-\\[\\]-\x{10FFFD}])|(?:(?:(?:${ECHAR}))|(?:(?:${UCHAR}))))))*\\'\\'\\'";
my $STRING_LITERAL_LONG2 = "\\\"\\\"\\\"(?:(?:(?:(?:(?:\\\")|(?:\\\"\\\")))?(?:(?:[\x{0000}-!#-\\[\\]-\x{10FFFD}])|(?:(?:(?:${ECHAR}))|(?:(?:${UCHAR}))))))*\\\"\\\"\\\"";
my $IRIREF = "<(?:(?:(?:[!#-;=\\?-\\[\\]_a-z~-\x{10FFFD}])|(?:(?:${UCHAR}))))*>";
my $PN_LOCAL_ESC = "\\\\(?:(?:_)|(?:(?:~)|(?:(?:\\.)|(?:(?:-)|(?:(?:!)|(?:(?:\\\$)|(?:(?:&)|(?:(?:\\')|(?:(?:\\()|(?:(?:\\))|(?:(?:\\*)|(?:(?:\\+)|(?:(?:,)|(?:(?:;)|(?:(?:=)|(?:(?:\\/)|(?:(?:\\?)|(?:(?:#)|(?:(?:\@)|(?:\%))))))))))))))))))))";
my $PLX = "(?:(?:${PERCENT}))|(?:(?:${PN_LOCAL_ESC}))";
my $PN_LOCAL = "(?:(?:(?:${PN_CHARS_U}))|(?:(?::)|(?:(?:[0-9])|(?:(?:${PLX})))))(?:(?:(?:(?:(?:(?:${PN_CHARS}))|(?:(?:\\.)|(?:(?::)|(?:(?:${PLX}))))))*(?:(?:(?:${PN_CHARS}))|(?:(?::)|(?:(?:${PLX}))))))?";
my $PNAME_LN = "(?:${PNAME_NS})(?:${PN_LOCAL})";
my $PASSED_TOKENS = "(?:(?:[\\t\\n\\r ])+)|(?:#(?:[\x{0000}-\\t\x{000B}-\x{000C}\x{000E}-\x{10FFFD}])*)";

my $Tokens = [[0, qr/$PASSED_TOKENS/, undef],
              [0, qr/$GT_DOT/i, 'GT_DOT'],
              [0, qr/$GT_SEMI/i, 'GT_SEMI'],
              [0, qr/$GT_COMMA/i, 'GT_COMMA'],
              [0, qr/$GT_LBRACKET/i, 'GT_LBRACKET'],
              [0, qr/$GT_RBRACKET/i, 'GT_RBRACKET'],
              [0, qr/$GT_LPAREN/i, 'GT_LPAREN'],
              [0, qr/$GT_RPAREN/i, 'GT_RPAREN'],
              [0, qr/$GT_DTYPE/i, 'GT_DTYPE'],
              [0, qr/$IT_true/i, 'IT_true'],
              [0, qr/$IT_false/i, 'IT_false'],
              [0, qr/$SPARQL_PREFIX/, 'SPARQL_PREFIX'],
              [0, qr/$SPARQL_BASE/, 'SPARQL_BASE'],
              [0, qr/$BASE/, 'BASE'],
              [0, qr/$PREFIX/, 'PREFIX'],
              [0, qr/$RDF_TYPE/, 'RDF_TYPE'],
              [0, qr/$IRIREF/, 'IRIREF'],
              [0, qr/$PNAME_NS/, 'PNAME_NS'],
              [0, qr/$PNAME_LN/, 'PNAME_LN'],
              [0, qr/$BLANK_NODE_LABEL/, 'BLANK_NODE_LABEL'],
              [0, qr/$LANGTAG/, 'LANGTAG'],
              [0, qr/$INTEGER/, 'INTEGER'],
              [0, qr/$DECIMAL/, 'DECIMAL'],
              [0, qr/$DOUBLE/, 'DOUBLE'],
              [0, qr/$STRING_LITERAL1/, 'STRING_LITERAL1'],
              [0, qr/$STRING_LITERAL2/, 'STRING_LITERAL2'],
              [0, qr/$STRING_LITERAL_LONG1/, 'STRING_LITERAL_LONG1'],
              [0, qr/$STRING_LITERAL_LONG2/, 'STRING_LITERAL_LONG2'],
              [0, qr/$ANON/, 'ANON'],
];
# END TokenBlock

# START ClassBlock
@turtleDoc::ISA = qw(_Production);
@_Qstatement_E_Star::ISA = qw(_GenProduction);
@statement::ISA = qw(_Production);
@directive::ISA = qw(_Production);
@prefixID::ISA = qw(_Production);
@base::ISA = qw(_Production);
@sparqlPrefix::ISA = qw(_Production);
@sparqlBase::ISA = qw(_Production);
@triples::ISA = qw(_Production);
@_QpredicateObjectList_E_Opt::ISA = qw(_GenProduction);
@predicateObjectList::ISA = qw(_Production);
@_O_Qverb_E_S_QobjectList_E_C::ISA = qw(_GenProduction);
@_Q_O_Qverb_E_S_QobjectList_E_C_E_Opt::ISA = qw(_GenProduction);
@_O_QGT_SEMI_E_S_Qverb_E_S_QobjectList_E_Opt_C::ISA = qw(_GenProduction);
@_Q_O_QGT_SEMI_E_S_Qverb_E_S_QobjectList_E_Opt_C_E_Star::ISA = qw(_GenProduction);
@objectList::ISA = qw(_Production);
@_O_QGT_COMMA_E_S_Qobject_E_C::ISA = qw(_GenProduction);
@_Q_O_QGT_COMMA_E_S_Qobject_E_C_E_Star::ISA = qw(_GenProduction);
@verb::ISA = qw(_Production);
@subject::ISA = qw(_Production);
@predicate::ISA = qw(_Production);
@object::ISA = qw(_Production);
@literal::ISA = qw(_Production);
@blankNodePropertyList::ISA = qw(_Production);
@collection::ISA = qw(_Production);
@_Qobject_E_Star::ISA = qw(_GenProduction);
@NumericLiteral::ISA = qw(_Production);
@RDFLiteral::ISA = qw(_Production);
@_O_QLANGTAG_E_Or_QGT_DTYPE_E_S_Qiri_E_C::ISA = qw(_GenProduction);
@_Q_O_QLANGTAG_E_Or_QGT_DTYPE_E_S_Qiri_E_C_E_Opt::ISA = qw(_GenProduction);
@BooleanLiteral::ISA = qw(_Production);
@String::ISA = qw(_Production);
@iri::ISA = qw(_Production);
@PrefixedName::ISA = qw(_Production);
@BlankNode::ISA = qw(_Production);

@GT_DOT::ISA = qw(_Constant);
@GT_SEMI::ISA = qw(_Constant);
@GT_COMMA::ISA = qw(_Constant);
@GT_LBRACKET::ISA = qw(_Constant);
@GT_RBRACKET::ISA = qw(_Constant);
@GT_LPAREN::ISA = qw(_Constant);
@GT_RPAREN::ISA = qw(_Constant);
@GT_DTYPE::ISA = qw(_Constant);
@IT_true::ISA = qw(_Constant);
@IT_false::ISA = qw(_Constant);
@SPARQL_PREFIX::ISA = qw(_Terminal);
@SPARQL_BASE::ISA = qw(_Terminal);
@BASE::ISA = qw(_Terminal);
@PREFIX::ISA = qw(_Terminal);
@RDF_TYPE::ISA = qw(_Terminal);
@IRIREF::ISA = qw(_Terminal);
@PNAME_NS::ISA = qw(_Terminal);
@PNAME_LN::ISA = qw(_Terminal);
@BLANK_NODE_LABEL::ISA = qw(_Terminal);
@LANGTAG::ISA = qw(_Terminal);
@INTEGER::ISA = qw(_Terminal);
@DECIMAL::ISA = qw(_Terminal);
@DOUBLE::ISA = qw(_Terminal);
@STRING_LITERAL1::ISA = qw(_Terminal);
@STRING_LITERAL2::ISA = qw(_Terminal);
@STRING_LITERAL_LONG1::ISA = qw(_Terminal);
@STRING_LITERAL_LONG2::ISA = qw(_Terminal);
@ANON::ISA = qw(_Terminal);

# END ClassBlock


sub new {
        my($class)=shift;
        ref($class)
    and $class=ref($class);

    my($self)=$class->SUPER::new( yyversion => '1.05',
                                  yystates =>
[
	{#State 0
		DEFAULT => -2,
		GOTOS => {
			'turtleDoc' => 1,
			'_Qstatement_E_Star' => 2
		}
	},
	{#State 1
		ACTIONS => {
			'' => 3
		}
	},
	{#State 2
		ACTIONS => {
			'BASE' => 4,
			'SPARQL_PREFIX' => 15,
			'GT_LPAREN' => 6,
			'PREFIX' => 18,
			'ANON' => 17,
			'GT_LBRACKET' => 21,
			'BLANK_NODE_LABEL' => 19,
			'PNAME_LN' => 23,
			'PNAME_NS' => 24,
			'SPARQL_BASE' => 26,
			'IRIREF' => 11
		},
		DEFAULT => -1,
		GOTOS => {
			'base' => 13,
			'sparqlBase' => 5,
			'sparqlPrefix' => 14,
			'subject' => 16,
			'collection' => 7,
			'PrefixedName' => 20,
			'prefixID' => 22,
			'triples' => 8,
			'blankNodePropertyList' => 9,
			'statement' => 10,
			'BlankNode' => 25,
			'directive' => 27,
			'iri' => 12
		}
	},
	{#State 3
		DEFAULT => 0
	},
	{#State 4
		ACTIONS => {
			'IRIREF' => 28
		}
	},
	{#State 5
		DEFAULT => -9
	},
	{#State 6
		DEFAULT => -45,
		GOTOS => {
			'_Qobject_E_Star' => 29
		}
	},
	{#State 7
		DEFAULT => -33
	},
	{#State 8
		ACTIONS => {
			'GT_DOT' => 30
		}
	},
	{#State 9
		ACTIONS => {
			'PNAME_LN' => 23,
			'PNAME_NS' => 24,
			'RDF_TYPE' => 36,
			'IRIREF' => 11
		},
		DEFAULT => -16,
		GOTOS => {
			'predicate' => 33,
			'verb' => 34,
			'_QpredicateObjectList_E_Opt' => 32,
			'predicateObjectList' => 31,
			'PrefixedName' => 20,
			'iri' => 35
		}
	},
	{#State 10
		DEFAULT => -3
	},
	{#State 11
		DEFAULT => -61
	},
	{#State 12
		DEFAULT => -31
	},
	{#State 13
		DEFAULT => -7
	},
	{#State 14
		DEFAULT => -8
	},
	{#State 15
		ACTIONS => {
			'PNAME_NS' => 37
		}
	},
	{#State 16
		ACTIONS => {
			'PNAME_LN' => 23,
			'PNAME_NS' => 24,
			'RDF_TYPE' => 36,
			'IRIREF' => 11
		},
		GOTOS => {
			'predicate' => 33,
			'verb' => 34,
			'predicateObjectList' => 38,
			'PrefixedName' => 20,
			'iri' => 35
		}
	},
	{#State 17
		DEFAULT => -66
	},
	{#State 18
		ACTIONS => {
			'PNAME_NS' => 39
		}
	},
	{#State 19
		DEFAULT => -65
	},
	{#State 20
		DEFAULT => -62
	},
	{#State 21
		ACTIONS => {
			'PNAME_LN' => 23,
			'PNAME_NS' => 24,
			'RDF_TYPE' => 36,
			'IRIREF' => 11
		},
		GOTOS => {
			'predicate' => 33,
			'verb' => 34,
			'predicateObjectList' => 40,
			'PrefixedName' => 20,
			'iri' => 35
		}
	},
	{#State 22
		DEFAULT => -6
	},
	{#State 23
		DEFAULT => -63
	},
	{#State 24
		DEFAULT => -64
	},
	{#State 25
		DEFAULT => -32
	},
	{#State 26
		ACTIONS => {
			'IRIREF' => 41
		}
	},
	{#State 27
		DEFAULT => -4
	},
	{#State 28
		ACTIONS => {
			'GT_DOT' => 42
		}
	},
	{#State 29
		ACTIONS => {
			'GT_LPAREN' => 6,
			'STRING_LITERAL_LONG2' => 45,
			'IT_true' => 48,
			'STRING_LITERAL_LONG1' => 49,
			'STRING_LITERAL2' => 52,
			'DECIMAL' => 53,
			'IRIREF' => 11,
			'INTEGER' => 56,
			'STRING_LITERAL1' => 57,
			'DOUBLE' => 58,
			'ANON' => 17,
			'BLANK_NODE_LABEL' => 19,
			'GT_LBRACKET' => 21,
			'PNAME_LN' => 23,
			'PNAME_NS' => 24,
			'GT_RPAREN' => 60,
			'IT_false' => 61
		},
		GOTOS => {
			'BooleanLiteral' => 43,
			'object' => 44,
			'String' => 46,
			'collection' => 47,
			'PrefixedName' => 20,
			'literal' => 50,
			'blankNodePropertyList' => 51,
			'BlankNode' => 59,
			'RDFLiteral' => 62,
			'NumericLiteral' => 54,
			'iri' => 55
		}
	},
	{#State 30
		DEFAULT => -5
	},
	{#State 31
		DEFAULT => -17
	},
	{#State 32
		DEFAULT => -15
	},
	{#State 33
		DEFAULT => -29
	},
	{#State 34
		ACTIONS => {
			'GT_LPAREN' => 6,
			'STRING_LITERAL_LONG2' => 45,
			'IT_true' => 48,
			'STRING_LITERAL_LONG1' => 49,
			'STRING_LITERAL2' => 52,
			'DECIMAL' => 53,
			'IRIREF' => 11,
			'INTEGER' => 56,
			'STRING_LITERAL1' => 57,
			'DOUBLE' => 58,
			'ANON' => 17,
			'BLANK_NODE_LABEL' => 19,
			'GT_LBRACKET' => 21,
			'PNAME_LN' => 23,
			'PNAME_NS' => 24,
			'IT_false' => 61
		},
		GOTOS => {
			'BooleanLiteral' => 43,
			'object' => 63,
			'String' => 46,
			'collection' => 47,
			'PrefixedName' => 20,
			'literal' => 50,
			'objectList' => 64,
			'blankNodePropertyList' => 51,
			'BlankNode' => 59,
			'RDFLiteral' => 62,
			'NumericLiteral' => 54,
			'iri' => 55
		}
	},
	{#State 35
		DEFAULT => -34
	},
	{#State 36
		DEFAULT => -30
	},
	{#State 37
		ACTIONS => {
			'IRIREF' => 65
		}
	},
	{#State 38
		DEFAULT => -14
	},
	{#State 39
		ACTIONS => {
			'IRIREF' => 66
		}
	},
	{#State 40
		ACTIONS => {
			'GT_RBRACKET' => 67
		}
	},
	{#State 41
		DEFAULT => -13
	},
	{#State 42
		DEFAULT => -11
	},
	{#State 43
		DEFAULT => -42
	},
	{#State 44
		DEFAULT => -46
	},
	{#State 45
		DEFAULT => -60
	},
	{#State 46
		ACTIONS => {
			'LANGTAG' => 68,
			'GT_DTYPE' => 70
		},
		DEFAULT => -53,
		GOTOS => {
			'_O_QLANGTAG_E_Or_QGT_DTYPE_E_S_Qiri_E_C' => 71,
			'_Q_O_QLANGTAG_E_Or_QGT_DTYPE_E_S_Qiri_E_C_E_Opt' => 69
		}
	},
	{#State 47
		DEFAULT => -37
	},
	{#State 48
		DEFAULT => -55
	},
	{#State 49
		DEFAULT => -59
	},
	{#State 50
		DEFAULT => -39
	},
	{#State 51
		DEFAULT => -38
	},
	{#State 52
		DEFAULT => -58
	},
	{#State 53
		DEFAULT => -48
	},
	{#State 54
		DEFAULT => -41
	},
	{#State 55
		DEFAULT => -35
	},
	{#State 56
		DEFAULT => -47
	},
	{#State 57
		DEFAULT => -57
	},
	{#State 58
		DEFAULT => -49
	},
	{#State 59
		DEFAULT => -36
	},
	{#State 60
		DEFAULT => -44
	},
	{#State 61
		DEFAULT => -56
	},
	{#State 62
		DEFAULT => -40
	},
	{#State 63
		DEFAULT => -27,
		GOTOS => {
			'_Q_O_QGT_COMMA_E_S_Qobject_E_C_E_Star' => 72
		}
	},
	{#State 64
		DEFAULT => -23,
		GOTOS => {
			'_Q_O_QGT_SEMI_E_S_Qverb_E_S_QobjectList_E_Opt_C_E_Star' => 73
		}
	},
	{#State 65
		DEFAULT => -12
	},
	{#State 66
		ACTIONS => {
			'GT_DOT' => 74
		}
	},
	{#State 67
		DEFAULT => -43
	},
	{#State 68
		DEFAULT => -51
	},
	{#State 69
		DEFAULT => -50
	},
	{#State 70
		ACTIONS => {
			'PNAME_LN' => 23,
			'PNAME_NS' => 24,
			'IRIREF' => 11
		},
		GOTOS => {
			'PrefixedName' => 20,
			'iri' => 75
		}
	},
	{#State 71
		DEFAULT => -54
	},
	{#State 72
		ACTIONS => {
			'GT_COMMA' => 76
		},
		DEFAULT => -25,
		GOTOS => {
			'_O_QGT_COMMA_E_S_Qobject_E_C' => 77
		}
	},
	{#State 73
		ACTIONS => {
			'GT_SEMI' => 78
		},
		DEFAULT => -18,
		GOTOS => {
			'_O_QGT_SEMI_E_S_Qverb_E_S_QobjectList_E_Opt_C' => 79
		}
	},
	{#State 74
		DEFAULT => -10
	},
	{#State 75
		DEFAULT => -52
	},
	{#State 76
		ACTIONS => {
			'GT_LPAREN' => 6,
			'STRING_LITERAL_LONG2' => 45,
			'IT_true' => 48,
			'STRING_LITERAL_LONG1' => 49,
			'STRING_LITERAL2' => 52,
			'DECIMAL' => 53,
			'IRIREF' => 11,
			'INTEGER' => 56,
			'STRING_LITERAL1' => 57,
			'DOUBLE' => 58,
			'ANON' => 17,
			'BLANK_NODE_LABEL' => 19,
			'GT_LBRACKET' => 21,
			'PNAME_LN' => 23,
			'PNAME_NS' => 24,
			'IT_false' => 61
		},
		GOTOS => {
			'BooleanLiteral' => 43,
			'object' => 80,
			'String' => 46,
			'collection' => 47,
			'PrefixedName' => 20,
			'literal' => 50,
			'blankNodePropertyList' => 51,
			'BlankNode' => 59,
			'RDFLiteral' => 62,
			'NumericLiteral' => 54,
			'iri' => 55
		}
	},
	{#State 77
		DEFAULT => -28
	},
	{#State 78
		ACTIONS => {
			'PNAME_LN' => 23,
			'PNAME_NS' => 24,
			'IRIREF' => 11,
			'RDF_TYPE' => 36
		},
		DEFAULT => -20,
		GOTOS => {
			'_O_Qverb_E_S_QobjectList_E_C' => 83,
			'_Q_O_Qverb_E_S_QobjectList_E_C_E_Opt' => 81,
			'predicate' => 33,
			'verb' => 82,
			'PrefixedName' => 20,
			'iri' => 35
		}
	},
	{#State 79
		DEFAULT => -24
	},
	{#State 80
		DEFAULT => -26
	},
	{#State 81
		DEFAULT => -22
	},
	{#State 82
		ACTIONS => {
			'GT_LPAREN' => 6,
			'STRING_LITERAL_LONG2' => 45,
			'IT_true' => 48,
			'STRING_LITERAL_LONG1' => 49,
			'STRING_LITERAL2' => 52,
			'DECIMAL' => 53,
			'IRIREF' => 11,
			'INTEGER' => 56,
			'STRING_LITERAL1' => 57,
			'DOUBLE' => 58,
			'ANON' => 17,
			'BLANK_NODE_LABEL' => 19,
			'GT_LBRACKET' => 21,
			'PNAME_LN' => 23,
			'PNAME_NS' => 24,
			'IT_false' => 61
		},
		GOTOS => {
			'BooleanLiteral' => 43,
			'object' => 63,
			'String' => 46,
			'collection' => 47,
			'PrefixedName' => 20,
			'literal' => 50,
			'objectList' => 84,
			'blankNodePropertyList' => 51,
			'BlankNode' => 59,
			'RDFLiteral' => 62,
			'NumericLiteral' => 54,
			'iri' => 55
		}
	},
	{#State 83
		DEFAULT => -21
	},
	{#State 84
		DEFAULT => -19
	}
],
                                  yyrules  =>
[
	[#Rule 0
		 '$start', 2, undef
	],
	[#Rule 1
		 'turtleDoc', 1,
sub
#line 151 "turtleAwesome.yp"
{
    my ($self, $_Qstatement_E_Star) = @_;
    my $ret = new turtleDoc($_Qstatement_E_Star);
    $self->traceProduction('turtleDoc', '_Qstatement_E_Star', $_Qstatement_E_Star);
    return $ret;
}
	],
	[#Rule 2
		 '_Qstatement_E_Star', 0,
sub
#line 159 "turtleAwesome.yp"
{
    my ($self, ) = @_;
    my $ret = new _Qstatement_E_Star();
    $self->traceProduction('_Qstatement_E_Star');
    return $ret;
}
	],
	[#Rule 3
		 '_Qstatement_E_Star', 2,
sub
#line 165 "turtleAwesome.yp"
{
    my ($self, $_Qstatement_E_Star, $statement) = @_;
    my $ret = new _Qstatement_E_Star($_Qstatement_E_Star, $statement);
    $self->traceProduction('_Qstatement_E_Star', '_Qstatement_E_Star', $_Qstatement_E_Star, 'statement', $statement);
    return $ret;
}
	],
	[#Rule 4
		 'statement', 1,
sub
#line 173 "turtleAwesome.yp"
{
    my ($self, $directive) = @_;
    my $ret = new statement($directive);
    $self->traceProduction('statement', 'directive', $directive);
    return $ret;
}
	],
	[#Rule 5
		 'statement', 2,
sub
#line 179 "turtleAwesome.yp"
{
    my ($self, $triples, $GT_DOT) = @_;
    my $ret = new statement($triples, $GT_DOT);
    $self->traceProduction('statement', 'triples', $triples, 'GT_DOT', $GT_DOT);
    return $ret;
}
	],
	[#Rule 6
		 'directive', 1,
sub
#line 187 "turtleAwesome.yp"
{
    my ($self, $prefixID) = @_;
    my $ret = new directive($prefixID);
    $self->traceProduction('directive', 'prefixID', $prefixID);
    return $ret;
}
	],
	[#Rule 7
		 'directive', 1,
sub
#line 193 "turtleAwesome.yp"
{
    my ($self, $base) = @_;
    my $ret = new directive($base);
    $self->traceProduction('directive', 'base', $base);
    return $ret;
}
	],
	[#Rule 8
		 'directive', 1,
sub
#line 199 "turtleAwesome.yp"
{
    my ($self, $sparqlPrefix) = @_;
    my $ret = new directive($sparqlPrefix);
    $self->traceProduction('directive', 'sparqlPrefix', $sparqlPrefix);
    return $ret;
}
	],
	[#Rule 9
		 'directive', 1,
sub
#line 205 "turtleAwesome.yp"
{
    my ($self, $sparqlBase) = @_;
    my $ret = new directive($sparqlBase);
    $self->traceProduction('directive', 'sparqlBase', $sparqlBase);
    return $ret;
}
	],
	[#Rule 10
		 'prefixID', 4,
sub
#line 218 "turtleAwesome.yp"
{
    my ($self, $PREFIX, $PNAME_NS, $IRIREF, $GT_DOT) = @_;
    my $ret = new prefixID($PREFIX, $PNAME_NS, $IRIREF, $GT_DOT);
    $self->traceProduction('prefixID', 'PREFIX', $PREFIX, 'PNAME_NS', $PNAME_NS, 'IRIREF', $IRIREF, 'GT_DOT', $GT_DOT);
    return $ret;
}
	],
	[#Rule 11
		 'base', 3,
sub
#line 226 "turtleAwesome.yp"
{
    my ($self, $BASE, $IRIREF, $GT_DOT) = @_;
    my $ret = new base($BASE, $IRIREF, $GT_DOT);
    $self->traceProduction('base', 'BASE', $BASE, 'IRIREF', $IRIREF, 'GT_DOT', $GT_DOT);
    return $ret;
}
	],
	[#Rule 12
		 'sparqlPrefix', 3,
sub
#line 239 "turtleAwesome.yp"
{
    my ($self, $SPARQL_PREFIX, $PNAME_NS, $IRIREF) = @_;
    my $ret = new sparqlPrefix($SPARQL_PREFIX, $PNAME_NS, $IRIREF);
    $self->traceProduction('sparqlPrefix', 'SPARQL_PREFIX', $SPARQL_PREFIX, 'PNAME_NS', $PNAME_NS, 'IRIREF', $IRIREF);
    return $ret;
}
	],
	[#Rule 13
		 'sparqlBase', 2,
sub
#line 247 "turtleAwesome.yp"
{
    my ($self, $SPARQL_BASE, $IRIREF) = @_;
    my $ret = new sparqlBase($SPARQL_BASE, $IRIREF);
    $self->traceProduction('sparqlBase', 'SPARQL_BASE', $SPARQL_BASE, 'IRIREF', $IRIREF);
    return $ret;
}
	],
	[#Rule 14
		 'triples', 2,
sub
#line 255 "turtleAwesome.yp"
{
    my ($self, $subject, $predicateObjectList) = @_;
    my $ret = new triples($subject, $predicateObjectList);
    $self->traceProduction('triples', 'subject', $subject, 'predicateObjectList', $predicateObjectList);
    return $ret;
}
	],
	[#Rule 15
		 'triples', 2,
sub
#line 261 "turtleAwesome.yp"
{
    my ($self, $blankNodePropertyList, $_QpredicateObjectList_E_Opt) = @_;
    my $ret = new triples($blankNodePropertyList, $_QpredicateObjectList_E_Opt);
    $self->traceProduction('triples', 'blankNodePropertyList', $blankNodePropertyList, '_QpredicateObjectList_E_Opt', $_QpredicateObjectList_E_Opt);
    return $ret;
}
	],
	[#Rule 16
		 '_QpredicateObjectList_E_Opt', 0,
sub
#line 269 "turtleAwesome.yp"
{
    my ($self, ) = @_;
    my $ret = new _QpredicateObjectList_E_Opt();
    $self->traceProduction('_QpredicateObjectList_E_Opt');
    return $ret;
}
	],
	[#Rule 17
		 '_QpredicateObjectList_E_Opt', 1,
sub
#line 275 "turtleAwesome.yp"
{
    my ($self, $predicateObjectList) = @_;
    my $ret = new _QpredicateObjectList_E_Opt($predicateObjectList);
    $self->traceProduction('_QpredicateObjectList_E_Opt', 'predicateObjectList', $predicateObjectList);
    return $ret;
}
	],
	[#Rule 18
		 'predicateObjectList', 3,
sub
#line 283 "turtleAwesome.yp"
{
    my ($self, $verb, $objectList, $_Q_O_QGT_SEMI_E_S_Qverb_E_S_QobjectList_E_Opt_C_E_Star) = @_;
    my $ret = new predicateObjectList($verb, $objectList, $_Q_O_QGT_SEMI_E_S_Qverb_E_S_QobjectList_E_Opt_C_E_Star);
    $self->traceProduction('predicateObjectList', 'verb', $verb, 'objectList', $objectList, '_Q_O_QGT_SEMI_E_S_Qverb_E_S_QobjectList_E_Opt_C_E_Star', $_Q_O_QGT_SEMI_E_S_Qverb_E_S_QobjectList_E_Opt_C_E_Star);
    return $ret;
}
	],
	[#Rule 19
		 '_O_Qverb_E_S_QobjectList_E_C', 2,
sub
#line 291 "turtleAwesome.yp"
{
    my ($self, $verb, $objectList) = @_;
    my $ret = new _O_Qverb_E_S_QobjectList_E_C($verb, $objectList);
    $self->traceProduction('_O_Qverb_E_S_QobjectList_E_C', 'verb', $verb, 'objectList', $objectList);
    return $ret;
}
	],
	[#Rule 20
		 '_Q_O_Qverb_E_S_QobjectList_E_C_E_Opt', 0,
sub
#line 299 "turtleAwesome.yp"
{
    my ($self, ) = @_;
    my $ret = new _Q_O_Qverb_E_S_QobjectList_E_C_E_Opt();
    $self->traceProduction('_Q_O_Qverb_E_S_QobjectList_E_C_E_Opt');
    return $ret;
}
	],
	[#Rule 21
		 '_Q_O_Qverb_E_S_QobjectList_E_C_E_Opt', 1,
sub
#line 305 "turtleAwesome.yp"
{
    my ($self, $_O_Qverb_E_S_QobjectList_E_C) = @_;
    my $ret = new _Q_O_Qverb_E_S_QobjectList_E_C_E_Opt($_O_Qverb_E_S_QobjectList_E_C);
    $self->traceProduction('_Q_O_Qverb_E_S_QobjectList_E_C_E_Opt', '_O_Qverb_E_S_QobjectList_E_C', $_O_Qverb_E_S_QobjectList_E_C);
    return $ret;
}
	],
	[#Rule 22
		 '_O_QGT_SEMI_E_S_Qverb_E_S_QobjectList_E_Opt_C', 2,
sub
#line 313 "turtleAwesome.yp"
{
    my ($self, $GT_SEMI, $_Q_O_Qverb_E_S_QobjectList_E_C_E_Opt) = @_;
    my $ret = new _O_QGT_SEMI_E_S_Qverb_E_S_QobjectList_E_Opt_C($GT_SEMI, $_Q_O_Qverb_E_S_QobjectList_E_C_E_Opt);
    $self->traceProduction('_O_QGT_SEMI_E_S_Qverb_E_S_QobjectList_E_Opt_C', 'GT_SEMI', $GT_SEMI, '_Q_O_Qverb_E_S_QobjectList_E_C_E_Opt', $_Q_O_Qverb_E_S_QobjectList_E_C_E_Opt);
    return $ret;
}
	],
	[#Rule 23
		 '_Q_O_QGT_SEMI_E_S_Qverb_E_S_QobjectList_E_Opt_C_E_Star', 0,
sub
#line 321 "turtleAwesome.yp"
{
    my ($self, ) = @_;
    my $ret = new _Q_O_QGT_SEMI_E_S_Qverb_E_S_QobjectList_E_Opt_C_E_Star();
    $self->traceProduction('_Q_O_QGT_SEMI_E_S_Qverb_E_S_QobjectList_E_Opt_C_E_Star');
    return $ret;
}
	],
	[#Rule 24
		 '_Q_O_QGT_SEMI_E_S_Qverb_E_S_QobjectList_E_Opt_C_E_Star', 2,
sub
#line 327 "turtleAwesome.yp"
{
    my ($self, $_Q_O_QGT_SEMI_E_S_Qverb_E_S_QobjectList_E_Opt_C_E_Star, $_O_QGT_SEMI_E_S_Qverb_E_S_QobjectList_E_Opt_C) = @_;
    my $ret = new _Q_O_QGT_SEMI_E_S_Qverb_E_S_QobjectList_E_Opt_C_E_Star($_Q_O_QGT_SEMI_E_S_Qverb_E_S_QobjectList_E_Opt_C_E_Star, $_O_QGT_SEMI_E_S_Qverb_E_S_QobjectList_E_Opt_C);
    $self->traceProduction('_Q_O_QGT_SEMI_E_S_Qverb_E_S_QobjectList_E_Opt_C_E_Star', '_Q_O_QGT_SEMI_E_S_Qverb_E_S_QobjectList_E_Opt_C_E_Star', $_Q_O_QGT_SEMI_E_S_Qverb_E_S_QobjectList_E_Opt_C_E_Star, '_O_QGT_SEMI_E_S_Qverb_E_S_QobjectList_E_Opt_C', $_O_QGT_SEMI_E_S_Qverb_E_S_QobjectList_E_Opt_C);
    return $ret;
}
	],
	[#Rule 25
		 'objectList', 2,
sub
#line 340 "turtleAwesome.yp"
{
    my ($self, $object, $_Q_O_QGT_COMMA_E_S_Qobject_E_C_E_Star) = @_;
    my $ret = new objectList($object, $_Q_O_QGT_COMMA_E_S_Qobject_E_C_E_Star);
    $self->traceProduction('objectList', 'object', $object, '_Q_O_QGT_COMMA_E_S_Qobject_E_C_E_Star', $_Q_O_QGT_COMMA_E_S_Qobject_E_C_E_Star);
    return $ret;
}
	],
	[#Rule 26
		 '_O_QGT_COMMA_E_S_Qobject_E_C', 2,
sub
#line 348 "turtleAwesome.yp"
{
    my ($self, $GT_COMMA, $object) = @_;
    my $ret = new _O_QGT_COMMA_E_S_Qobject_E_C($GT_COMMA, $object);
    $self->traceProduction('_O_QGT_COMMA_E_S_Qobject_E_C', 'GT_COMMA', $GT_COMMA, 'object', $object);
    return $ret;
}
	],
	[#Rule 27
		 '_Q_O_QGT_COMMA_E_S_Qobject_E_C_E_Star', 0,
sub
#line 356 "turtleAwesome.yp"
{
    my ($self, ) = @_;
    my $ret = new _Q_O_QGT_COMMA_E_S_Qobject_E_C_E_Star();
    $self->traceProduction('_Q_O_QGT_COMMA_E_S_Qobject_E_C_E_Star');
    return $ret;
}
	],
	[#Rule 28
		 '_Q_O_QGT_COMMA_E_S_Qobject_E_C_E_Star', 2,
sub
#line 362 "turtleAwesome.yp"
{
    my ($self, $_Q_O_QGT_COMMA_E_S_Qobject_E_C_E_Star, $_O_QGT_COMMA_E_S_Qobject_E_C) = @_;
    my $ret = new _Q_O_QGT_COMMA_E_S_Qobject_E_C_E_Star($_Q_O_QGT_COMMA_E_S_Qobject_E_C_E_Star, $_O_QGT_COMMA_E_S_Qobject_E_C);
    $self->traceProduction('_Q_O_QGT_COMMA_E_S_Qobject_E_C_E_Star', '_Q_O_QGT_COMMA_E_S_Qobject_E_C_E_Star', $_Q_O_QGT_COMMA_E_S_Qobject_E_C_E_Star, '_O_QGT_COMMA_E_S_Qobject_E_C', $_O_QGT_COMMA_E_S_Qobject_E_C);
    return $ret;
}
	],
	[#Rule 29
		 'verb', 1,
sub
#line 370 "turtleAwesome.yp"
{
    my ($self, $predicate) = @_;
    my $ret = new verb($predicate);
    $self->traceProduction('verb', 'predicate', $predicate);
    return $ret;
}
	],
	[#Rule 30
		 'verb', 1,
sub
#line 376 "turtleAwesome.yp"
{
    my ($self, $RDF_TYPE) = @_;
    my $ret = new verb($RDF_TYPE);
    $self->traceProduction('verb', 'RDF_TYPE', $RDF_TYPE);
    return $ret;
}
	],
	[#Rule 31
		 'subject', 1,
sub
#line 384 "turtleAwesome.yp"
{
    my ($self, $iri) = @_;
    my $ret = new subject($iri);
    $self->traceProduction('subject', 'iri', $iri);
    return $ret;
}
	],
	[#Rule 32
		 'subject', 1,
sub
#line 390 "turtleAwesome.yp"
{
    my ($self, $BlankNode) = @_;
    my $ret = new subject($BlankNode);
    $self->traceProduction('subject', 'BlankNode', $BlankNode);
    return $ret;
}
	],
	[#Rule 33
		 'subject', 1,
sub
#line 396 "turtleAwesome.yp"
{
    my ($self, $collection) = @_;
    my $ret = new subject($collection);
    $self->traceProduction('subject', 'collection', $collection);
    return $ret;
}
	],
	[#Rule 34
		 'predicate', 1,
sub
#line 404 "turtleAwesome.yp"
{
    my ($self, $iri) = @_;
    my $ret = new predicate($iri);
    $self->traceProduction('predicate', 'iri', $iri);
    return $ret;
}
	],
	[#Rule 35
		 'object', 1,
sub
#line 412 "turtleAwesome.yp"
{
    my ($self, $iri) = @_;
    my $ret = new object($iri);
    $self->traceProduction('object', 'iri', $iri);
    return $ret;
}
	],
	[#Rule 36
		 'object', 1,
sub
#line 418 "turtleAwesome.yp"
{
    my ($self, $BlankNode) = @_;
    my $ret = new object($BlankNode);
    $self->traceProduction('object', 'BlankNode', $BlankNode);
    return $ret;
}
	],
	[#Rule 37
		 'object', 1,
sub
#line 424 "turtleAwesome.yp"
{
    my ($self, $collection) = @_;
    my $ret = new object($collection);
    $self->traceProduction('object', 'collection', $collection);
    return $ret;
}
	],
	[#Rule 38
		 'object', 1,
sub
#line 430 "turtleAwesome.yp"
{
    my ($self, $blankNodePropertyList) = @_;
    my $ret = new object($blankNodePropertyList);
    $self->traceProduction('object', 'blankNodePropertyList', $blankNodePropertyList);
    return $ret;
}
	],
	[#Rule 39
		 'object', 1,
sub
#line 436 "turtleAwesome.yp"
{
    my ($self, $literal) = @_;
    my $ret = new object($literal);
    $self->traceProduction('object', 'literal', $literal);
    return $ret;
}
	],
	[#Rule 40
		 'literal', 1,
sub
#line 444 "turtleAwesome.yp"
{
    my ($self, $RDFLiteral) = @_;
    my $ret = new literal($RDFLiteral);
    $self->traceProduction('literal', 'RDFLiteral', $RDFLiteral);
    return $ret;
}
	],
	[#Rule 41
		 'literal', 1,
sub
#line 450 "turtleAwesome.yp"
{
    my ($self, $NumericLiteral) = @_;
    my $ret = new literal($NumericLiteral);
    $self->traceProduction('literal', 'NumericLiteral', $NumericLiteral);
    return $ret;
}
	],
	[#Rule 42
		 'literal', 1,
sub
#line 456 "turtleAwesome.yp"
{
    my ($self, $BooleanLiteral) = @_;
    my $ret = new literal($BooleanLiteral);
    $self->traceProduction('literal', 'BooleanLiteral', $BooleanLiteral);
    return $ret;
}
	],
	[#Rule 43
		 'blankNodePropertyList', 3,
sub
#line 464 "turtleAwesome.yp"
{
    my ($self, $GT_LBRACKET, $predicateObjectList, $GT_RBRACKET) = @_;
    my $ret = new blankNodePropertyList($GT_LBRACKET, $predicateObjectList, $GT_RBRACKET);
    $self->traceProduction('blankNodePropertyList', 'GT_LBRACKET', $GT_LBRACKET, 'predicateObjectList', $predicateObjectList, 'GT_RBRACKET', $GT_RBRACKET);
    return $ret;
}
	],
	[#Rule 44
		 'collection', 3,
sub
#line 472 "turtleAwesome.yp"
{
    my ($self, $GT_LPAREN, $_Qobject_E_Star, $GT_RPAREN) = @_;
    my $ret = new collection($GT_LPAREN, $_Qobject_E_Star, $GT_RPAREN);
    $self->traceProduction('collection', 'GT_LPAREN', $GT_LPAREN, '_Qobject_E_Star', $_Qobject_E_Star, 'GT_RPAREN', $GT_RPAREN);
    return $ret;
}
	],
	[#Rule 45
		 '_Qobject_E_Star', 0,
sub
#line 480 "turtleAwesome.yp"
{
    my ($self, ) = @_;
    my $ret = new _Qobject_E_Star();
    $self->traceProduction('_Qobject_E_Star');
    return $ret;
}
	],
	[#Rule 46
		 '_Qobject_E_Star', 2,
sub
#line 486 "turtleAwesome.yp"
{
    my ($self, $_Qobject_E_Star, $object) = @_;
    my $ret = new _Qobject_E_Star($_Qobject_E_Star, $object);
    $self->traceProduction('_Qobject_E_Star', '_Qobject_E_Star', $_Qobject_E_Star, 'object', $object);
    return $ret;
}
	],
	[#Rule 47
		 'NumericLiteral', 1,
sub
#line 494 "turtleAwesome.yp"
{
    my ($self, $INTEGER) = @_;
    my $ret = new NumericLiteral($INTEGER);
    $self->traceProduction('NumericLiteral', 'INTEGER', $INTEGER);
    return $ret;
}
	],
	[#Rule 48
		 'NumericLiteral', 1,
sub
#line 500 "turtleAwesome.yp"
{
    my ($self, $DECIMAL) = @_;
    my $ret = new NumericLiteral($DECIMAL);
    $self->traceProduction('NumericLiteral', 'DECIMAL', $DECIMAL);
    return $ret;
}
	],
	[#Rule 49
		 'NumericLiteral', 1,
sub
#line 506 "turtleAwesome.yp"
{
    my ($self, $DOUBLE) = @_;
    my $ret = new NumericLiteral($DOUBLE);
    $self->traceProduction('NumericLiteral', 'DOUBLE', $DOUBLE);
    return $ret;
}
	],
	[#Rule 50
		 'RDFLiteral', 2,
sub
#line 514 "turtleAwesome.yp"
{
    my ($self, $String, $_Q_O_QLANGTAG_E_Or_QGT_DTYPE_E_S_Qiri_E_C_E_Opt) = @_;
    my $ret = new RDFLiteral($String, $_Q_O_QLANGTAG_E_Or_QGT_DTYPE_E_S_Qiri_E_C_E_Opt);
    $self->traceProduction('RDFLiteral', 'String', $String, '_Q_O_QLANGTAG_E_Or_QGT_DTYPE_E_S_Qiri_E_C_E_Opt', $_Q_O_QLANGTAG_E_Or_QGT_DTYPE_E_S_Qiri_E_C_E_Opt);
    return $ret;
}
	],
	[#Rule 51
		 '_O_QLANGTAG_E_Or_QGT_DTYPE_E_S_Qiri_E_C', 1,
sub
#line 522 "turtleAwesome.yp"
{
    my ($self, $LANGTAG) = @_;
    my $ret = new _O_QLANGTAG_E_Or_QGT_DTYPE_E_S_Qiri_E_C($LANGTAG);
    $self->traceProduction('_O_QLANGTAG_E_Or_QGT_DTYPE_E_S_Qiri_E_C', 'LANGTAG', $LANGTAG);
    return $ret;
}
	],
	[#Rule 52
		 '_O_QLANGTAG_E_Or_QGT_DTYPE_E_S_Qiri_E_C', 2,
sub
#line 528 "turtleAwesome.yp"
{
    my ($self, $GT_DTYPE, $iri) = @_;
    my $ret = new _O_QLANGTAG_E_Or_QGT_DTYPE_E_S_Qiri_E_C($GT_DTYPE, $iri);
    $self->traceProduction('_O_QLANGTAG_E_Or_QGT_DTYPE_E_S_Qiri_E_C', 'GT_DTYPE', $GT_DTYPE, 'iri', $iri);
    return $ret;
}
	],
	[#Rule 53
		 '_Q_O_QLANGTAG_E_Or_QGT_DTYPE_E_S_Qiri_E_C_E_Opt', 0,
sub
#line 536 "turtleAwesome.yp"
{
    my ($self, ) = @_;
    my $ret = new _Q_O_QLANGTAG_E_Or_QGT_DTYPE_E_S_Qiri_E_C_E_Opt();
    $self->traceProduction('_Q_O_QLANGTAG_E_Or_QGT_DTYPE_E_S_Qiri_E_C_E_Opt');
    return $ret;
}
	],
	[#Rule 54
		 '_Q_O_QLANGTAG_E_Or_QGT_DTYPE_E_S_Qiri_E_C_E_Opt', 1,
sub
#line 542 "turtleAwesome.yp"
{
    my ($self, $_O_QLANGTAG_E_Or_QGT_DTYPE_E_S_Qiri_E_C) = @_;
    my $ret = new _Q_O_QLANGTAG_E_Or_QGT_DTYPE_E_S_Qiri_E_C_E_Opt($_O_QLANGTAG_E_Or_QGT_DTYPE_E_S_Qiri_E_C);
    $self->traceProduction('_Q_O_QLANGTAG_E_Or_QGT_DTYPE_E_S_Qiri_E_C_E_Opt', '_O_QLANGTAG_E_Or_QGT_DTYPE_E_S_Qiri_E_C', $_O_QLANGTAG_E_Or_QGT_DTYPE_E_S_Qiri_E_C);
    return $ret;
}
	],
	[#Rule 55
		 'BooleanLiteral', 1,
sub
#line 550 "turtleAwesome.yp"
{
    my ($self, $IT_true) = @_;
    my $ret = new BooleanLiteral($IT_true);
    $self->traceProduction('BooleanLiteral', 'IT_true', $IT_true);
    return $ret;
}
	],
	[#Rule 56
		 'BooleanLiteral', 1,
sub
#line 556 "turtleAwesome.yp"
{
    my ($self, $IT_false) = @_;
    my $ret = new BooleanLiteral($IT_false);
    $self->traceProduction('BooleanLiteral', 'IT_false', $IT_false);
    return $ret;
}
	],
	[#Rule 57
		 'String', 1,
sub
#line 566 "turtleAwesome.yp"
{
    my ($self, $STRING_LITERAL1) = @_;
    my $ret = new String($STRING_LITERAL1);
    $self->traceProduction('String', 'STRING_LITERAL1', $STRING_LITERAL1);
    return $ret;
}
	],
	[#Rule 58
		 'String', 1,
sub
#line 572 "turtleAwesome.yp"
{
    my ($self, $STRING_LITERAL2) = @_;
    my $ret = new String($STRING_LITERAL2);
    $self->traceProduction('String', 'STRING_LITERAL2', $STRING_LITERAL2);
    return $ret;
}
	],
	[#Rule 59
		 'String', 1,
sub
#line 578 "turtleAwesome.yp"
{
    my ($self, $STRING_LITERAL_LONG1) = @_;
    my $ret = new String($STRING_LITERAL_LONG1);
    $self->traceProduction('String', 'STRING_LITERAL_LONG1', $STRING_LITERAL_LONG1);
    return $ret;
}
	],
	[#Rule 60
		 'String', 1,
sub
#line 584 "turtleAwesome.yp"
{
    my ($self, $STRING_LITERAL_LONG2) = @_;
    my $ret = new String($STRING_LITERAL_LONG2);
    $self->traceProduction('String', 'STRING_LITERAL_LONG2', $STRING_LITERAL_LONG2);
    return $ret;
}
	],
	[#Rule 61
		 'iri', 1,
sub
#line 592 "turtleAwesome.yp"
{
    my ($self, $IRIREF) = @_;
    my $ret = new iri($IRIREF);
    $self->traceProduction('iri', 'IRIREF', $IRIREF);
    return $ret;
}
	],
	[#Rule 62
		 'iri', 1,
sub
#line 598 "turtleAwesome.yp"
{
    my ($self, $PrefixedName) = @_;
    my $ret = new iri($PrefixedName);
    $self->traceProduction('iri', 'PrefixedName', $PrefixedName);
    return $ret;
}
	],
	[#Rule 63
		 'PrefixedName', 1,
sub
#line 606 "turtleAwesome.yp"
{
    my ($self, $PNAME_LN) = @_;
    my $ret = new PrefixedName($PNAME_LN);
    $self->traceProduction('PrefixedName', 'PNAME_LN', $PNAME_LN);
    return $ret;
}
	],
	[#Rule 64
		 'PrefixedName', 1,
sub
#line 612 "turtleAwesome.yp"
{
    my ($self, $PNAME_NS) = @_;
    my $ret = new PrefixedName($PNAME_NS);
    $self->traceProduction('PrefixedName', 'PNAME_NS', $PNAME_NS);
    return $ret;
}
	],
	[#Rule 65
		 'BlankNode', 1,
sub
#line 620 "turtleAwesome.yp"
{
    my ($self, $BLANK_NODE_LABEL) = @_;
    my $ret = new BlankNode($BLANK_NODE_LABEL);
    $self->traceProduction('BlankNode', 'BLANK_NODE_LABEL', $BLANK_NODE_LABEL);
    return $ret;
}
	],
	[#Rule 66
		 'BlankNode', 1,
sub
#line 626 "turtleAwesome.yp"
{
    my ($self, $ANON) = @_;
    my $ret = new BlankNode($ANON);
    $self->traceProduction('BlankNode', 'ANON', $ANON);
    return $ret;
}
	]
],
                                  @_);
    bless($self,$class);
}

#line 701 "turtleAwesome.yp"
 #*** Additional Code ***

my $LanguageName = 'turtleAwesome';
# -*- Mode: cperl; coding: utf-8; cperl-indent-level: 4 -*-
# START LexerBlock
#
# YappTemplate: used by yacker to create yapp input files.
#
# Use: yacker -l perl -s -n <name> <name>.txt
#
# to generate a yapp input module called turtleAwesome.yp.

#line 11 "YappTemplate"

# $Id: Langname_.yp,v 1.1 2008/04/08 09:34:09 eric Exp $

sub _Base::new {
    my ($proto, @args) = @_;
    my $class = ref($proto) || $proto;
    my $self = [];
    foreach my $arg (@args) {
	if (UNIVERSAL::isa($arg, $class)) {

	    # Collapse nested left-recursive *, +, ? and () productions.
	    push (@$self, @$arg);
	} else {

	    # Construct simple parse tree of production parameters.
	    push (@$self, $arg);
	}
    }
    bless ($self, $class);
    return $self;
}
sub _Base::toString {
    my ($self) = @_;
    my @ret = map {$_->toString} @$self;
    return wantarray ? @ret : join(' ', @ret);
}
sub _Base::toXML {
    my ($self, $prefix, $decls) = @_;
    my $class = ref $self;
    my $declsStr = join('', map {my $p = $_ ? ":$_" : ''; "\n xmlns$p=\"$decls->{$_}\""} keys %$decls);
    my @ret = ("$prefix<$class$declsStr>", map {ref $_ ? $_->toXML("$prefix  ", {}) : $_} @$self, "$prefix</$class>");
    return wantarray ? @ret : join("\n", @ret);
}

@_Production::ISA = qw(_Base);
@_GenProduction::ISA = qw(_Production);
sub _GenProduction::toXML {
    my ($self, $prefix) = @_;
    return join("\n", map {$_->toXML($prefix)} @$self);
}

@_Terminal::ISA = qw(_Base);
sub _Terminal::toString {
    my ($self) = @_;
    my $encodedValue = $self->[0];
    $encodedValue =~ s/\r/\\r/g;
    $encodedValue =~ s/\n/\\n/g;
    $encodedValue =~ s/\t/\\t/g;
    return $encodedValue;
}
sub _Terminal::toXML {
    my ($self, $prefix) = @_;
    my $class = ref $self;
    my $encodedValue = $self->[0];
    $encodedValue =~ s/&/&amp;/g;
    $encodedValue =~ s/</&lt;/g;
    $encodedValue =~ s/>/&gt;/g;
    return "$prefix<$class>$encodedValue</$class>";
}
@_Constant::ISA = qw(_Base);
sub _Constant::toString {
    my ($self) = @_;
    return ($self->[0]);
}
sub _Constant::toXML {
    my ($self, $prefix) = @_;
    my $class = ref $self;
    $class =~ s/^[IG]T_//;
    return "$prefix<yacker:implicit-terminal>$class</yacker:implicit-terminal>";
}

sub _Error {
    my ($self) = @_;
        exists $self->YYData->{ERRMSG}
    and do {
        print $self->YYData->{ERRMSG};
        delete $self->YYData->{ERRMSG};
        return;
    };
    my $pos = pos $self->YYData->{INPUT};
    my $lastPos = $self->YYData->{my_LASTPOS};
    my $excerpt = substr($self->YYData->{INPUT}, $lastPos, $pos - $lastPos);
    my $expect = @{$self->{STACK}} ? join (' | ', sort {(!(lc $a cmp lc $b)) ? $b cmp $a : lc $a cmp lc $b} map {&_terminalString($_)} $self->YYExpect()) : 'INVALID INITIALIZER';
    if (ref $expect) {
	# Flag unexpected (by the author at this point) refs with '?ref'.
	if (ref $expect eq 'HASH') {
	    if (exists $expect->{NEXT}) {
		$expect = $ {$expect->{NEXT}};
	    } else {
		$expect = "?ref {%$expect}";
	    }
	} elsif (ref $expect eq 'ARRAY') {
	    $expect = "?ref [@$expect]";
	} elsif (ref $expect eq 'SCALAR') {
	    $expect = "?ref $$expect";
	} elsif (ref $expect eq 'GLOB') {
	    $expect = "?ref \**$expect";
	} else {
	    $expect = "?ref ??? $expect";
	}
    }
    my $token = &_terminalString($self->YYData->{my_LASTTOKEN});
    my $value = $self->YYData->{my_LASTVALUE};
    die "expected \"$expect\", got ($token, $value) from \"$excerpt\" at offset $lastPos.\n";
}

sub _terminalString { # static
    my ($token) = @_;
    if ($token =~ m{^I_T_(.+)$}) {
	$token = "'$1'";
    } elsif ($token =~ m{^T_(.+)$}) {
	if (my $base = $ARGV[0]) {
	    $token = "&lt;<a href=\"$base$token\">$1</a>&gt;";
	} else {
	    $token = "<$1>";
	}
    }
    return $token;
}

my $AtStart;

sub _Lexer {
    my($self)=shift;

    my ($token, $value) = ('', undef);

  top:
    if (defined $self->YYData->{INPUT} && 
	pos $self->YYData->{INPUT} < length ($self->YYData->{INPUT})) {
	# still some chars left.
    } else {
	return ('', undef);
    }

    $self->YYData->{my_LASTPOS} = pos $self->YYData->{INPUT};
    my $startPos = pos $self->YYData->{INPUT};
    my ($mText, $mLen, $mI, $mLookAhead) = ('', 0, undef, undef);
    for (my $i = 0; $i < @$Tokens; $i++) {
	my $rule = $Tokens->[$i];
	my ($start, $regexp, $action) = @$rule;
	if ($start && !$AtStart) {
	    next;
	}
	eval {
	    if ($self->YYData->{INPUT} =~ m/\G($regexp)/gc) {
		my $lookAhead = defined $2 ? length $2 : 0;
		my $len = (pos $self->YYData->{INPUT}) - $startPos + $lookAhead;
		if ($len > $mLen) {
		    $mText = substr($self->YYData->{INPUT}, $startPos, $len - $lookAhead);
		    $mLen = $len;
		    $mI = $i;
		    $mLookAhead = $lookAhead
		}
		pos $self->YYData->{INPUT} = $startPos;
	    }
	}; if ($@) {
	    die "error processing $action: $@";
	}
    }
    if ($mLen) {
	my ($start, $regexp, $action) = @{$Tokens->[$mI]};
	pos $self->YYData->{INPUT} += $mLen - $mLookAhead;
	$AtStart = $mText =~ m/\z/gc;
	($token, $value) = ($action, $mText);
    } else {
	my $excerpt = substr($self->YYData->{INPUT}, pos $self->YYData->{INPUT}, 40);
	die "lexer couldn't parse at \"$excerpt\"\n";
    }
    if (!defined $token) {
	# We just parsed whitespace or comment.
	goto top;
    }
#    my $pos = pos $self->YYData->{INPUT};
#    print "\n$pos,$token,$value\n";
    $self->YYData->{my_LASTTOKEN} = $token;
    $self->YYData->{my_LASTVALUE} = $value;
    my $ret = $token->new($value);
    my $str = $ret->toString;
    $self->trace("shift ($token, $str)");
    return ($token, $ret);
}

# END LexerBlock

sub parse {
    my ($self, $sample) = @_;
    $self->YYData->{INPUT} = $sample;
    pos $self->YYData->{INPUT} = 0;
    return $self->YYParse( yylex => \&_Lexer, yyerror => \&_Error, yydebug => $ENV{YYDEBUG} );
}

sub openTraceFd {
    my ($self, $fd) = @_;
    open $self->YYData->{Trace}, '>&', $fd;
}
sub closeTrace {
    my ($self, $fd) = @_;
    close $self->YYData->{Trace};
}
sub trace {
    my($self, $str) = @_;
    if ($self->YYData->{Trace}) {
	&utf8::encode($str);
	print {$self->YYData->{Trace}} "$str\n";
    }
}
sub traceProduction {
    my($self, $prod, @parms) = @_;
    if ($self->YYData->{Trace}) {
	my $str = "  $prod:";
	my @lines;
	while (@parms) {
	    my ($parmName, $parmVal) = (shift @parms, shift @parms);

	    if (UNIVERSAL::isa($parmVal, '_GenProduction')) {

		# Enumerate elements of *, +, ? and () productions.
		$str .= sprintf(" %s(%d)", $parmName, scalar @$parmVal);
		for (my $i = 0; $i < @$parmVal; $i++) {
		    push (@lines, sprintf("    %s(%d): %s", $parmName, $i, join(' ', $parmVal->[$i]->toString)));
		}
	    } else {

		# Display singleton properties via their toString form.
		$str .= sprintf(" %s(%d)", $parmName, 1);
		push (@lines, sprintf("    %s(%d): %s", $parmName, 0, join(' ', $parmVal->toString)));
	    }
	}
	$str = join("\n", $str, @lines);  
	&utf8::encode($str);
	print {$self->YYData->{Trace}} "$str\n";
    }
}

require Exporter;
use vars qw ( @EXPORT );
push (@ISA, qw ( Exporter ));
@EXPORT = qw(&test);

sub test {
    if (@ARGV < 1) {
	local $/ = undef;
	&testFile(<STDIN>, $ENV{TRACE_FD});
    } else {
	foreach my $file (@ARGV) {
	    open(F, $file) || die "unable to open input $file: $!\n";
	    local $/ = undef;
	    &testFile(<F>, $ENV{TRACE_FD});
	    close (F);
	}
    }
}
sub testFile {
    my ($sample, $traceFd) = @_;
    my $parser = turtleAwesome->new();
    &utf8::decode($sample);
    if ($ENV{TRACE_FD}) {
	$parser->openTraceFd($ENV{TRACE_FD});
    }
    eval {
	my $root = $parser->parse($sample);
	my $text = $root->toXML('', {
	 '' => 'http://www.w3.org/2005/01/yacker/uploads/turtleAwesome/', 
	 'yacker' => 'http://www.w3.org/2005/01/yacker/'});

	# @@@ you may need to comment this for command line processing.
	&utf8::encode($text);

	print "$text\n";
    };
    my $lastError = $@;
    if ($ENV{TRACE_FD}) {
	$parser->closeTrace();
    }
    if ($lastError) {
	die $lastError;
    }
}

1;

__END__

=head1 turtleAwesome

turtleAwesome - parse some language.

=head1 SYNOPSIS

    my ($sample) = $ARGV[0];
    &utf8::decode($sample);
    my $parser = new turtleAwesome();
    my $root = $parser->parser($sample);
    my $text = $root->toXML('', {
	 '' => 'http://www.w3.org/2005/01/yacker/uploads/turtleAwesome/', 
	 'yacker' => 'http://www.w3.org/2005/01/yacker/'});
    &utf8::encode($text);
    print "$text\n";

=head1 DESCRIPTION

Yacker needs to encode rule patterns in [a-zA-Z_]+ so it reserves symbols starting with '_'. This parser reverses the process.

This module was generated by W3C::Grammar::bin::yacker.


=head1 API

This function supplies a single parsing function. The methods of the returned object are described below.

=head2 parse($sample)

Returns an array of objects parsed into the language given to yacker.

=head2 returned object

The returned objects are blessed subclasses of _Production. They have the following functions:

=head3 toString

Return a ' '-separated "normalization" of the parsed $sample.

=head3 toXML

Return an XML parse tree of the parsed $sample.


=head1 TESTING/DEBUGGING

    TRACE_FD=3 perl -MturtleAwesome -e test < sample.in 3> sample.trace
or
    TRACE_FD=3 perl -MturtleAwesome -e test sample 3> sample.trace

which should return a parse tree for the given language.

Setting the trace file descriptor to 1 will send the trace output to stdout.
    TRACE_FD=1
Leaving it unset will suppress the trace output.


=head1 BUGS

The web interface to yacker requires the results to be encoded:
  &utf8::encode($text)

Many shells do not expect this so you may need to comment it out. You
may search for the "@@@" above to find the line in sub test.


=head1 AUTHOR

turtleAwesome author: unknown
yacker author: Eric Prud'hommeaux <eric@w3.org>

=head1 SEE ALSO

W3C::Grammar::bin::yacker(1)

=cut


1;
