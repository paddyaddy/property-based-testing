% reverse(reverse(s)) == s
violation :- source_in(S), followup_out(T), S != T.
#show violation/0.
