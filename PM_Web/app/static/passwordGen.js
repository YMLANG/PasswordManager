var characters = [
    "abcdefghijklmnopqrstuvwxyz",
    "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
    "0123456789",
    "!@#$%^&*()-=_+}[]<>,."
];
function updatePasswordStrength() {
    const password = document.getElementById('password').value,
    passwordLength = document.getElementById('password').value.length;
    let lowerCaseUsed = false,
    upperCaseUsed = false,
    numberUsed = false,
    specialCharacterUsed = false;

    let i = passwordLength;
    while (i--) {
    if (characters[0].includes(password.charAt(i))) { lowerCaseUsed = true; continue; }
    if (characters[1].includes(password.charAt(i))) { upperCaseUsed = true; continue; }
    if (characters[2].includes(password.charAt(i))) { numberUsed = true; continue; }
    if (characters[3].includes(password.charAt(i))) { specialCharacterUsed = true; }
    }

    let complexity = 0;
    if (lowerCaseUsed) { complexity += characters[0].length; }
    if (upperCaseUsed) { complexity += characters[1].length; }
    if (numberUsed) { complexity += characters[2].length; }
    if (specialCharacterUsed) { complexity += characters[3].length; }
    complexity = Math.pow(complexity, passwordLength);

    const strength = document.getElementById('password_strength');
    if (complexity < Math.pow(26, 16)) { 
    strength.value = 'Weak';
    strength.style.backgroundColor = "red";
    } else if (complexity >= Math.pow(26, 16) && complexity < Math.pow(26,32)) {
    strength.value = 'Moderate';
    strength.style.backgroundColor = "yellow";
    } else if (complexity >= Math.pow(26, 32)) { 
    strength.value = 'Strong';
    strength.style.backgroundColor = "green";
    }
}

window.onload = () => {
    document.getElementById('password_strength').value = 'Weak';
    document.getElementById('password').addEventListener('input', () => {
        updatePasswordStrength();
    });
}

function generateRandomNumber(min, max) {
    var array = new Uint16Array(1);
    window.crypto.getRandomValues(array);

    var range = max - min + 1;
    var max_range = 65536;

    if (array[0] >= Math.floor(max_range/range) * range) { return generateRandomNumber(min, max); }
    return min + (array[0] % range);
};

function generatePassword() {
    var password_length = document.getElementById("password_length").value,
    special_symbols = document.getElementById("special_symbols").checked,
    upper_case = document.getElementById("upper_case").checked,
    numbers = document.getElementById("numbers").checked,
    password = "",
    chars_to_use = characters[0];
    if (password_length === "" || parseInt(password_length) == 0) { password_length = "16"; }
    if (special_symbols) { chars_to_use += characters[3]; }
    if (upper_case) { chars_to_use += characters[1]; }
    if (numbers) { chars_to_use += characters[2]; }

    for (i = 0; i < parseInt(password_length); i++) {
        password += chars_to_use.charAt(generateRandomNumber(0, chars_to_use.length));
    }

    document.getElementById("password").value=password;
    updatePasswordStrength();
    return false;
};