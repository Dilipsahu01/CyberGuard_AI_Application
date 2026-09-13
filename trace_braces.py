with open('app/src/main/java/com/example/IncomingCallActivity.kt', 'r') as f:
    lines = f.readlines()

open_c = 0
for i, line in enumerate(lines):
    open_c += line.count('{')
    open_c -= line.count('}')
    if 480 <= i <= 510:
        print(f"L{i+1}: c={open_c} | {line.strip()}")
