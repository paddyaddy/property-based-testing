n(N) :- N = #count { P : source_out(P, _) }.
expected(Pos, EV) :- source_out(Q, V), n(N), Pos = N - 1 - Q, EV = -V.
violation :- expected(P, EV), followup_out(P, FV), EV != FV.
expected_has(P) :- expected(P, _).
followup_has(P) :- followup_out(P, _).
violation :- expected(P, _),     not followup_has(P).
violation :- followup_out(P, _), not expected_has(P).
#show violation/0.
