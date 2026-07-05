import os
import re
import shutil

src_dir = "NewUI_UX"
dest_dir = "app/src/main/java/com/example/ui"

if not os.path.exists(dest_dir):
    os.makedirs(dest_dir)

for filename in os.listdir(src_dir):
    if filename.endswith(".kt"):
        src_path = os.path.join(src_dir, filename)
        dest_path = os.path.join(dest_dir, filename)
        
        with open(src_path, "r") as f:
            content = f.read()
            
        content = re.sub(r'package com\.cyberguardai\.dialer\.ui', 'package com.example.ui', content)
        
        with open(dest_path, "w") as f:
            f.write(content)

print("Files moved and packages updated successfully.")
