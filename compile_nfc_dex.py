import os
import subprocess
import shutil

base_dir = os.path.dirname(os.path.abspath(__file__))
build_dir = os.path.join(base_dir, 'nfc_build')
classes_dir = os.path.join(build_dir, 'classes')
os.makedirs(classes_dir, exist_ok=True)

def find_tool(name):
    w = shutil.which(name)
    if w: return w
    java_home = os.environ.get('JAVA_HOME', '')
    if java_home:
        cand = os.path.join(java_home, 'bin', f'{name}.exe' if os.name == 'nt' else name)
        if os.path.exists(cand): return cand
    default_ms_jdk = r'C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot\bin'
    cand = os.path.join(default_ms_jdk, f'{name}.exe')
    if os.path.exists(cand): return cand
    return name

javac = find_tool('javac')
java = find_tool('java')
r8 = os.path.join(base_dir, 'build-tools', 'r8.jar')
android_jar = os.path.join(base_dir, 'build-tools', 'android.jar')

src_dir = os.path.join(base_dir, 'nfc_helper_source')
src1 = os.path.join(src_dir, 'NfcFix.java')
src2 = os.path.join(src_dir, 'AndroidNfcBackend.java')

print("1. Compiling Java...")
res = subprocess.run([javac, '-encoding', 'UTF-8', '-source', '8', '-target', '8', '-d', classes_dir, src1, src2], capture_output=True, text=True)
print("javac:", res.returncode, res.stdout, res.stderr)
if res.returncode != 0:
    exit(1)

print("2. Converting to DEX with D8...")
dex_dir = os.path.join(build_dir, 'dex')
os.makedirs(dex_dir, exist_ok=True)
class_files = [os.path.join(classes_dir, f) for f in os.listdir(classes_dir) if f.endswith('.class')]
res = subprocess.run([java, '-cp', r8, 'com.android.tools.r8.D8', '--release', '--min-api', '26', '--lib', android_jar, '--output', dex_dir] + class_files, capture_output=True, text=True)
print("D8:", res.returncode, res.stdout, res.stderr)
if res.returncode != 0:
    exit(1)

out_dex = os.path.join(dex_dir, 'classes.dex')
target_assets = os.path.join(base_dir, 'ResolutionMasterApp', 'assets')
os.makedirs(target_assets, exist_ok=True)
target_dex = os.path.join(target_assets, 'nfc-fix.dex')

shutil.copyfile(out_dex, target_dex)
print(f"SUCCESS! Built enhanced DEX ({os.path.getsize(target_dex)} bytes) at {target_dex}")
