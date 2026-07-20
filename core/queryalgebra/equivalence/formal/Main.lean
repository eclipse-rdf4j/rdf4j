import Rdf4jEquivalenceFormal

open Rdf4jEquivalenceFormal

private partial def checkStream (stream : IO.FS.Stream) : IO (Nat × Nat × Nat) := do
  let rec loop (accepted rejected oracleCases lineNumber : Nat) : IO (Nat × Nat × Nat) := do
    let rawLine ← stream.getLine
    if rawLine.isEmpty then
      return (accepted, rejected, oracleCases)
    let lineNumber := lineNumber + 1
    let line := rawLine.trimAscii.toString
    if line.isEmpty then
      loop accepted rejected oracleCases lineNumber
    else
      match checkCertificateLineWithOracle line with
      | .ok certificate =>
          loop (accepted + 1) rejected (oracleCases + oracleCaseCount certificate) lineNumber
      | .error reason =>
          IO.println s!"REJECT line={lineNumber}: {reason}"
          loop accepted (rejected + 1) oracleCases lineNumber
  loop 0 0 0 0

def main (arguments : List String) : IO UInt32 := do
  let (accepted, rejected, oracleCases) ← match arguments with
    | [] => checkStream (← IO.getStdin)
    | [path] => IO.FS.withFile path IO.FS.Mode.read fun handle =>
        checkStream (IO.FS.Stream.ofHandle handle)
    | _ =>
        IO.eprintln "usage: check-certificates [json-lines-file]"
        return 2
  if rejected == 0 then
    IO.println s!"ACCEPT certificates={accepted} oracleCases={oracleCases}"
    return 0
  else
    IO.println s!"REJECT certificates={rejected}"
    return 1
