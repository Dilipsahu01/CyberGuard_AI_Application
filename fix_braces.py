import re

with open('app/src/main/java/com/example/IncomingCallActivity.kt', 'r') as f:
    lines = f.readlines()

banner_idx = -1
for i, line in enumerate(lines):
    if "private fun CallWaitingBanner" in line:
        banner_idx = i - 1
        while "@Composable" not in lines[banner_idx]:
            banner_idx -= 1
        break

end_ringing_idx = -1
for i in range(banner_idx, -1, -1):
    if "                            )" in lines[i]:
        end_ringing_idx = i
        break

print("banner_idx:", banner_idx)
print("end_ringing_idx:", end_ringing_idx)

# Replace everything between end_ringing_idx and banner_idx with proper closing braces
# We need to calculate how many braces are needed to leave exactly 1 open (the class)
open_count = 0
close_count = 0
for i in range(end_ringing_idx + 1):
    open_count += lines[i].count('{')
    close_count += lines[i].count('}')

diff = open_count - close_count
braces_needed = diff - 1

print(f"Need {braces_needed} braces.")

new_lines = lines[:end_ringing_idx + 1]
for i in range(braces_needed):
    spaces = "    " * (braces_needed - i)
    new_lines.append(f"{spaces}}}\n")
new_lines.append("\n")
new_lines.extend(lines[banner_idx:])

with open('app/src/main/java/com/example/IncomingCallActivity.kt', 'w') as f:
    f.writelines(new_lines)
print("Fixed!")
