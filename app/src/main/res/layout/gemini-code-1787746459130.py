import tkinter as tk
from tkinter import ttk
import subprocess
import threading

class HebrewTerminalApp:
    def __init__(self, root):
        self.root = root
        self.root.title("מסוף כרטיסיות עברי")
        self.root.geometry("800x500")
        
        self.notebook = ttk.Notebook(root)
        self.notebook.pack(fill=tk.BOTH, expand=True)
        
        self.add_tab("כרטיסייה 1")

    def add_tab(self, title):
        frame = ttk.Frame(self.notebook)
        self.notebook.add(frame, text=title)
        
        # תיבת טקסט
        text_area = tk.Text(frame, bg="black", fg="white", insertbackground="white", font=("Consolas", 12))
        text_area.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        
        # פס גלילה
        scrollbar = ttk.Scrollbar(frame, orient=tk.VERTICAL, command=text_area.yview)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        text_area.configure(yscrollcommand=scrollbar.set)

        # הפעלת תהליך ה-CMD
        process = subprocess.Popen(
            "cmd.exe",
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            stdin=subprocess.PIPE,
            shell=True,
            text=True,
            encoding='cp862', # קידוד התומך בעברית ישנה בקונסול
            bufsize=1
        )
        
        text_area.process = process
        text_area.mark_set("input_start", "insert")

        # קריאת פלט מהתהליך ברקע
        def read_output():
            while True:
                output = process.stdout.readline()
                if not output and process.poll() is not None:
                    break
                if output:
                    text_area.after(0, lambda o=output: self.append_output(text_area, o))

        threading.Thread(target=read_output, daemon=True).start()

        # האזנה למקש Enter לשליחת פקודה
        text_area.bind("<Return>", lambda event: self.send_command(event, text_area))

    def append_output(self, text_area, text):
        text_area.insert(tk.END, text)
        text_area.see(tk.END)
        text_area.mark_set("input_start", tk.END)

    def send_command(self, event, text_area):
        # שליפת הטקסט שהמשתמש הקליד מאז הפקודה האחרונה
        command = text_area.get("input_start", tk.END).strip()
        
        if text_area.process:
            text_area.process.stdin.write(command + "\n")
            text_area.process.stdin.flush()
            
        text_area.mark_set("input_start", tk.END)
        return # מאפשר מעבר שורה רגיל אחרי ה-Enter

if __name__ == "__main__":
    root = tk.Tk()
    app = HebrewTerminalApp(root)
    root.mainloop()