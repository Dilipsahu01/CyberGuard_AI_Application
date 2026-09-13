with open('app/src/main/java/com/example/IncomingCallActivity.kt', 'r') as f:
    lines = f.readlines()

open_c = 0
for i, line in enumerate(lines):
    open_c += line.count('{')
    open_c -= line.count('}')
    if open_c == 0 and i > 100:
        print(f"Brace reached 0 at line {i+1}: {line.strip()}")
        if i > 290:
            break
