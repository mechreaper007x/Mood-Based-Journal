import os

# Configuration
SOURCE_DIR = r"c:\Users\Savyasachi Mishra\Desktop\Mood based journal\moodjournal\moodjournal\backend\src\main\java"
# OUTPUT_FILE = r"C:\Users\Savyasachi Mishra\.gemini\antigravity\brain\9c049e11-d52a-45ed-9aba-d6d9b60f19bd\full_project_codebase.md"
OUTPUT_FILE = "export.md"

def export_codebase():
    with open(OUTPUT_FILE, 'w', encoding='utf-8') as outfile:
        outfile.write("# Mood Journal Backend - Full Source Code\n\n")
        
        for root, dirs, files in os.walk(SOURCE_DIR):
            for file in files:
                if file.endswith(".java"):
                    filepath = os.path.join(root, file)
                    relpath = os.path.relpath(filepath, SOURCE_DIR)
                    
                    outfile.write(f"## {relpath}\n")
                    outfile.write("```java\n")
                    
                    try:
                        with open(filepath, 'r', encoding='utf-8') as infile:
                            outfile.write(infile.read())
                    except Exception as e:
                        outfile.write(f"// Error reading file: {e}\n")
                        
                    outfile.write("\n```\n\n")
                    print(f"Exported: {relpath}")

if __name__ == "__main__":
    export_codebase()
    print(f"Done! Saved to {OUTPUT_FILE}")
