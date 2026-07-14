import os
import shutil

old_pkg = "com.example.quranmaster"
new_pkg = "com.sabah.bikhushue"

def replace_in_files(directory):
    for root, dirs, files in os.walk(directory):
        if "build" in root.split(os.sep):
            continue
        for file in files:
            if file.endswith(('.kt', '.xml', '.gradle', '.pro', '.json', '.txt')):
                filepath = os.path.join(root, file)
                try:
                    with open(filepath, 'r', encoding='utf-8') as f:
                        content = f.read()
                    if old_pkg in content:
                        new_content = content.replace(old_pkg, new_pkg)
                        with open(filepath, 'w', encoding='utf-8') as f:
                            f.write(new_content)
                        print(f"Updated {filepath}")
                except Exception as e:
                    print(f"Failed to process {filepath}: {e}")

app_dir = r"c:\quran.master\app"
replace_in_files(app_dir)

# Now move the directory
old_dir = os.path.join(app_dir, "src", "main", "java", "com", "example", "quranmaster")
new_dir = os.path.join(app_dir, "src", "main", "java", "com", "sabah", "bikhushue")

if os.path.exists(old_dir):
    os.makedirs(new_dir, exist_ok=True)
    for item in os.listdir(old_dir):
        shutil.move(os.path.join(old_dir, item), new_dir)
    print("Moved files to new directory structure")
    
    # Try to clean up old dirs if empty
    try:
        os.rmdir(old_dir)
        os.rmdir(os.path.join(app_dir, "src", "main", "java", "com", "example"))
    except OSError:
        pass
else:
    print("Old directory not found, maybe already moved?")
