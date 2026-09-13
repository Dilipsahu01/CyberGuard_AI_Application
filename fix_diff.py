with open('app/src/main/java/com/example/IncomingCallActivity.kt', 'r') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if "private fun CallWaitingBanner" in line:
        # remove one of the '}' lines above it
        for j in range(i-1, -1, -1):
            if "}" in lines[j]:
                lines.pop(j)
                break
        break

with open('app/src/main/java/com/example/IncomingCallActivity.kt', 'w') as f:
    f.writelines(lines)
