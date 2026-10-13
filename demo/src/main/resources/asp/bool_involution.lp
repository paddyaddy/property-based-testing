% not(not(b)) == b: apply not twice via metamorphic, source_in should equal followup_out
violation :- source_in(B), followup_out(O), B != O.
#show violation/0.
