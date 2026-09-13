with open('app/src/main/java/com/example/IncomingCallActivity.kt', 'r') as f:
    text = f.read()
open_c = text.count('{')
close_c = text.count('}')
print(f"Open: {open_c}, Close: {close_c}, Diff: {open_c - close_c}")
