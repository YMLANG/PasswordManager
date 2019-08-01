from flask import Flask, render_template, flash, session, request, redirect, url_for, g, send_file
from werkzeug.utils import secure_filename
import pyrebase
import os
import hashlib
import base64
import pyAesCrypt
from Crypto.Cipher import AES

app = Flask(__name__)
app.secret_key = "Dev secret key"

# create a temp directory for uploading and downloading files
os.system('mkdir ./temp')

UPLOAD_FOLDER = './temp'
ALLOWED_EXTENSIONS = set(['ks'])
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER

# config for connecting to firebase
config = {
    "apiKey": "AIzaSyBdT0UZQD_YmeyC32ffzQCCQfL6DwcGEa0",
    "authDomain": "keystore-22d55.firebaseapp.com",
    "databaseURL": "https://keystore-22d55.firebaseio.com",
    "projectId": "keystore-22d55",
    "storageBucket": "keystore-22d55.appspot.com",
    "messagingSenderId": "413934096515"
}

firebase = pyrebase.initialize_app(config)
db = firebase.database()
auth = firebase.auth()

# make sure only files with ".ks" extension are allowed
def allowed_file(filename):
    return '.' in filename and \
           filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

# helper function to encrypt strings to send to the db
def encrypt(string):
    encryption = AES.new('This is a key123', AES.MODE_CBC, 'This is an IV123')
    while len(string) % 16 != 0:
        string = string + "{"
    string = encryption.encrypt(string)
    return string

# helper function to decrypt strings received from the db
def decrypt(cipher_text):
    encryption = AES.new('This is a key123', AES.MODE_CBC, 'This is an IV123')
    cipher_text = base64.b64decode(cipher_text)
    plain_text = encryption.decrypt(cipher_text)
    return plain_text.decode('utf-8')

# helper function to encrypt backup file
def encrypt_user_accounts():
    buffer_size = 64*1024
    password = hashlib.sha256(session.get('user_id').encode('utf-8')).hexdigest()
    with open("./temp/temp", "w+") as temp:
        for account_details in session.get('user_accounts'):
            for key, value in account_details.items():
                temp.write(key + "\n")
                for _, value2 in value.items():
                    temp.write(value2 + "\n")
                temp.write("\n")
    pyAesCrypt.encryptFile("./temp/temp", hashlib.sha256(session.get('user_id').encode('utf-8')).hexdigest() + ".ks", password, buffer_size)

# helper function to decrypt user backup file
def restore_user_accounts():
    buffer_size = 64*1024
    password = hashlib.sha256(session.get('user_id').encode('utf-8')).hexdigest()
    pyAesCrypt.decryptFile("./temp/" + hashlib.sha256(session.get('user_id').encode('utf-8')).hexdigest() + ".ks", "./temp/temp", password, buffer_size)

    with open("./temp/temp", 'r') as user_file:
        temp = user_file.read().splitlines()
        if len(temp) == 0:
            return False
        data = {"account": str(base64.b64encode(encrypt(temp[1])), 'utf-8'), 
                "password": str(base64.b64encode(encrypt(temp[2])), 'utf-8')}
        db.child("users").child(session.get('user_id')).child(temp[0]).set(data)
        return True

def get_accounts():
    user_accounts = db.child("users").get()
    process_data(user_accounts)


# receive the data from the db and process it so it can be sent to front end
def process_data(user_data):
    user_data = user_data.val()
    session['user_accounts'] = []
    if user_data is None:
        return
    for key, value in user_data.items():
        if key == session.get('user_id'):
            for _, value2 in value.items():
                for key2, value3 in value2.items():
                    value2[key2] = decrypt(value3).replace("{", "")
            session.get('user_accounts').append(value)

def sign_in(email, password):
    try:
        user_auth = auth.sign_in_with_email_and_password(email, password)
        email = hashlib.sha256(email.encode('utf-8')).hexdigest()
        idToken = user_auth['idToken']
        refreshToken = user_auth['refreshToken']
        user_info = auth.get_account_info(idToken)
        user_id = user_info['users'][0]['localId']
        session['idToken'] = idToken
        session['refreshToken'] = refreshToken
        session['email'] = email
        session['user_accounts'] = []
        session['user_id'] = user_id
        get_accounts()
        return True
    except:
        return False

def create_account(email, password):
    user = auth.create_user_with_email_and_password(email, password)
    auth.send_email_verification(user['idToken'])

@app.route("/register", methods=['POST', 'GET'])
def register():
    if request.method == "POST":
        back = request.form.get('back')
        if back is not None and back == "Back to homepage":
            return redirect(url_for("login"))
        if request.form['password'] != request.form['confirm_password']:
            flash("Passwords do not match")
            return render_template("register2.html")
        try:
            create_account(request.form['email'], request.form['password'])
            flash("An email has been sent to you to verify your account. Please verify your account before logging in.")
            return redirect(url_for("login"))
        except:
            flash("Account already exists")
            return render_template("register2.html")
    else:
        return render_template("register2.html")

@app.route("/upload_backup", methods=['POST', 'GET'])
def upload_backup():
    if request.method == "POST":
        if 'file' not in request.files:
            flash('No file uploaded')
            return redirect(url_for('upload_backup'))
        file = request.files['file']
        if file.filename == '':
            flash('No selected file')
            return redirect(url_for('upload_backup'))
        if file.filename != hashlib.sha256(session.get('user_id').encode('utf-8')).hexdigest() + ".ks":
            flash('Invalid File')
            return redirect(url_for('upload_backup'))
        if file and allowed_file(file.filename):
            filename = secure_filename(file.filename)
            file.save(os.path.join(app.config['UPLOAD_FOLDER'], filename))
            if restore_user_accounts():
                return redirect(url_for('index'))
            else:
                flash("Invalid file")
                return redirect(url_for('upload_backup'))
        else:
            flash("Invalid file")
            return redirect(url_for('upload_backup'))
    else:
        return render_template("upload_backup.html")

@app.route("/", methods=['POST', 'GET'])
def index():
    if session.get('logged_in'):
        if request.method == "POST":
            download = request.form.get('download')
            logout = request.form.get('logout')
            add_account = request.form.get('add_account')
            restore = request.form.get('restore')
            if logout is not None and logout == "Logout":
                session['logged_in'] = False
                return render_template("login2.html")
            elif add_account is not None and add_account == "Add an account":
                return redirect(url_for("add_account"))
            elif download is not None and download == "Download a copy of your accounts":
                try:
                    encrypt_user_accounts()
                    return send_file(hashlib.sha256(session.get('user_id').encode('utf-8')).hexdigest() + ".ks", mimetype='application/txt', as_attachment=True)
                except Exception as e:
                    return str(e)
            elif restore is not None and restore == "Restore accounts from backup":
                return redirect(url_for('upload_backup'))
            else:
                get_accounts()
                return render_template("profile.html", user_accounts=session.get('user_accounts'))
        else:
            get_accounts()
            return render_template("profile.html", user_accounts=session.get('user_accounts'))
    return login()

@app.route("/forgot_password", methods=['POST', 'GET'])
def forgot_password():
    if request.method == "POST":
        try:
            auth.send_password_reset_email(request.form['email'])
            flash("An email to reset your password has been sent to you")
            return redirect(url_for('login'))
        except:
            flash("Invalid email")
            return redirect(url_for('forgot_password'))
    else:
        return render_template("forgot_password.html")

@app.route("/", methods=['POST', 'GET'])
def login():
    if request.method == "POST":
        register = request.form.get('register')
        login = request.form.get('login')
        forgot = request.form.get('forgot')
        if forgot is not None and forgot == "Forgot Password":
            return redirect(url_for('forgot_password'))
        if register is not None and register == "Register":
            return redirect(url_for('register')) 
        if login is not None and login == "Login":
            if sign_in(request.form['email'], request.form['password']):
                user_info = auth.get_account_info(session.get('idToken'))
                if not user_info['users'][0]['emailVerified']:
                    flash("Email not verified")
                    return render_template("login2.html")
                session['logged_in'] = True
                return render_template("profile.html", user_accounts=session.get('user_accounts'))
            else:
                flash("Incorrect login details")
                return render_template("login2.html")
    return render_template("login2.html")

@app.route("/add_account", methods=['POST', 'GET'])
def add_account():
    if session.get('logged_in'):
        if request.method == "POST":
            cancel = request.form.get('cancel')
            if cancel is not None and cancel == "Cancel":
                return redirect(url_for('index'))
            else:
                service = request.form.get('service')
                if service is None:
                    flash("Please fill out the details")
                    return redirect(url_for('add_account'))
                service = request.form['service']
                user = str(base64.b64encode(encrypt(request.form['user'])), 'utf-8')
                password = str(base64.b64encode(encrypt(request.form['password'])), 'utf-8')
                data = {"account" : user,
                        "password" : password}
                db.child("users").child(session.get('user_id')).child(service).set(data)
                get_accounts()
                return redirect(url_for("index"))
        else:
            return render_template("add_account2.html")
    else:
        return "Not authorised"


if __name__ == "__main__":
    app.jinja_env.globals.update(decrypt=decrypt)
    app.run(host='127.0.0.1', debug=True, port=80)
