violation :- source_out(P, V1), followup_out(P, V2), V1 != V2.
source_has(P) :- source_out(P, _).
followup_has(P) :- followup_out(P, _).
violation :- source_out(P, _), not followup_has(P).
violation :- followup_out(P, _), not source_has(P).
#show violation/0.
