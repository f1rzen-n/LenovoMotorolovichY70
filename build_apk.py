import subprocess
import os
import shutil
import zipfile
import glob

base_dir = os.path.dirname(os.path.abspath(__file__))
app_dir = os.path.join(base_dir, 'ResolutionMasterApp')
bt_dir = os.path.join(base_dir, 'build-tools')
shizuku_dir = os.path.join(base_dir, 'shizuku_libs')
bin_dir = os.path.join(app_dir, 'bin')
shutil.rmtree(bin_dir, ignore_errors=True)
os.makedirs(bin_dir, exist_ok=True)

def find_tool(name):
    # Check PATH first
    w = shutil.which(name)
    if w:
        return w
    java_home = os.environ.get('JAVA_HOME', '')
    if java_home:
        cand = os.path.join(java_home, 'bin', f'{name}.exe' if os.name == 'nt' else name)
        if os.path.exists(cand):
            return cand
    default_ms_jdk = r'C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot\bin'
    cand = os.path.join(default_ms_jdk, f'{name}.exe')
    if os.path.exists(cand):
        return cand
    return name

javac = find_tool('javac')
java = find_tool('java')
keytool = find_tool('keytool')

aapt2 = os.path.join(bt_dir, 'aapt2.exe')
r8 = os.path.join(bt_dir, 'r8.jar')
android_jar = os.path.join(bt_dir, 'android.jar')
apksig_jar = os.path.join(bt_dir, 'apksig.jar')

compiled_res = os.path.join(app_dir, 'compiled_res.zip')
if os.path.exists(compiled_res):
    os.remove(compiled_res)

unaligned_apk = os.path.join(app_dir, 'ResolutionMaster_unaligned.apk')
if os.path.exists(unaligned_apk):
    os.remove(unaligned_apk)

signed_apk = os.path.join(app_dir, 'ResolutionMaster_signed.apk')
if os.path.exists(signed_apk):
    os.remove(signed_apk)

manifest = os.path.join(app_dir, 'AndroidManifest.xml')
res_dir = os.path.join(app_dir, 'res')
src_dir = os.path.join(app_dir, 'src')

# Step 1: aapt2 compile
print("1. Compiling resources with aapt2...")
res = subprocess.run([aapt2, 'compile', '--dir', res_dir, '-o', compiled_res], capture_output=True, text=True, encoding='utf-8')
print("aapt2 compile:", res.stdout, res.stderr)
if res.returncode != 0:
    exit(1)

# Step 2: aapt2 link
print("2. Linking resources and generating R.java...")
res = subprocess.run([aapt2, 'link', '-I', android_jar, '--manifest', manifest, '-o', unaligned_apk, '--java', src_dir, compiled_res, '--auto-add-overlay'], capture_output=True, text=True, encoding='utf-8')
print("aapt2 link:", res.stdout, res.stderr)
if res.returncode != 0:
    exit(1)

# Collect Shizuku JARs
shizuku_jars = sorted(glob.glob(os.path.join(shizuku_dir, '*.jar')))
print("Found Shizuku JARs:", [os.path.basename(j) for j in shizuku_jars])
javac_cp = f"{android_jar};{';'.join(shizuku_jars)}"

# Step 3: Compile Java with javac (-encoding UTF-8)
print("3. Compiling Java sources with javac (UTF-8)...")
java_pkg_dir = os.path.join(src_dir, 'com', 'moto', 'resolutionmaster')
java_files = [os.path.join(java_pkg_dir, f) for f in os.listdir(java_pkg_dir) if f.endswith('.java')]
res = subprocess.run([javac, '-encoding', 'UTF-8', '-cp', javac_cp, '-d', bin_dir] + java_files, capture_output=True, text=True, encoding='utf-8')
print("javac:", res.stdout, res.stderr)
if res.returncode != 0:
    exit(1)

# Step 4: D8 dex compilation
print("4. Compiling classes to DEX with D8...")
class_pkg_dir = os.path.join(bin_dir, 'com', 'moto', 'resolutionmaster')
class_files = [os.path.join(class_pkg_dir, f) for f in os.listdir(class_pkg_dir) if f.endswith('.class')]
d8_cmd = [java, '-cp', r8, 'com.android.tools.r8.D8', '--min-api', '26', '--lib', android_jar, '--output', bin_dir] + class_files + shizuku_jars
res = subprocess.run(d8_cmd, capture_output=True, text=True, encoding='utf-8')
print("D8:", res.stdout, res.stderr)
if res.returncode != 0:
    exit(1)

# Step 5: Add classes.dex and assets into APK
print("5. Adding classes.dex and assets into APK...")
dex_file = os.path.join(bin_dir, 'classes.dex')
assets_dir = os.path.join(app_dir, 'assets')
with zipfile.ZipFile(unaligned_apk, 'a') as apk:
    apk.write(dex_file, 'classes.dex')
    if os.path.exists(assets_dir):
        for root, dirs, files in os.walk(assets_dir):
            for f in files:
                full_path = os.path.join(root, f)
                rel_path = os.path.relpath(full_path, app_dir).replace('\\', '/')
                apk.write(full_path, rel_path)
                print(f"   Added {rel_path} ({os.path.getsize(full_path)} bytes)")

# Step 6: Create debug keystore if not exists
keystore = os.path.join(base_dir, 'debug.keystore')
if not os.path.exists(keystore):
    print("6. Creating debug keystore...")
    subprocess.run([keytool, '-genkey', '-v', '-keystore', keystore, '-storepass', 'android', '-alias', 'androiddebugkey', '-keypass', 'android', '-keyalg', 'RSA', '-keysize', '2048', '-validity', '10000', '-dname', 'CN=Android Debug,O=Android,C=US'], capture_output=True)

# Step 7: Sign APK with ApkSigner (v1, v2, v3)
print("7. Signing APK with v1, v2, v3 schemes...")
sign_apk_class = os.path.join(base_dir, 'SignApk.class')
sign_apk_java = os.path.join(base_dir, 'SignApk.java')
if not os.path.exists(sign_apk_class):
    subprocess.run([javac, '-cp', apksig_jar, sign_apk_java], check=True)

res = subprocess.run([java, '-cp', f'{apksig_jar};{base_dir}', 'SignApk', unaligned_apk, signed_apk, keystore, 'android', 'androiddebugkey'], capture_output=True, text=True, encoding='utf-8')
print("ApkSigner:", res.stdout, res.stderr)
if res.returncode != 0:
    exit(1)

# Copy final release artifact
release_apk_v29 = os.path.join(base_dir, 'LenovoMotorolovichY70_v2.9.apk')
shutil.copy2(signed_apk, release_apk_v29)

print("\nSUCCESS! APK built and signed successfully at:")
print(signed_apk)
print("Release v2.9 copy created at:", release_apk_v29)
print("APK file size:", os.path.getsize(release_apk_v29), "bytes")
