// ============ FETCH API EXAMPLE ============

const API_URL = "http://localhost:8080/api/auth/register";

/**
 * Example 1: Simple fetch with correct headers and error handling
 */
async function registerUserFetch() {
    const userData = {
        fullName: "Alice Example",
        email: "alice@example.com",
        password: "P@ssw0rd123",
        workspaceName: "My Awesome Workspace",
        workspaceSlug: "my-awesome-workspace"
    };

    try {
        const response = await fetch(API_URL, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(userData)
        });

        if (response.ok) {
            const data = await response.json();
            console.log("Registration successful:", data);
            // Handle success: navigate to login, show message, etc.
        } else if (response.status === 400) {
            // Validation error
            const error = await response.json();
            console.error("Validation failed:", error);

            // Example: Display field errors to user
            if (error.errors && Array.isArray(error.errors)) {
                error.errors.forEach(fieldError => {
                    console.error(`${fieldError.field} (${fieldError.code}): ${fieldError.message}`);
                });
            }
        } else if (response.status === 409) {
            // Conflict (email or workspace slug already taken)
            const error = await response.json();
            console.error("Conflict:", error.message);
        } else {
            const error = await response.json();
            console.error("Unexpected error:", error);
        }
    } catch (error) {
        console.error("Network error:", error.message);
    }
}

// ============ AXIOS EXAMPLE (if you use axios) ============

/**
 * Example 2: Using axios library (add <script src="https://cdn.jsdelivr.net/npm/axios/dist/axios.min.js"></script>)
 */
async function registerUserAxios() {
    const userData = {
        fullName: "Bob Developer",
        email: "bob@dev.com",
        password: "Secure@1234",
        workspaceName: "Dev Workspace",
        workspaceSlug: "dev-workspace"
    };

    try {
        const response = await axios.post(API_URL, userData, {
            headers: {
                "Content-Type": "application/json"
            }
        });

        console.log("Registration successful:", response.data);
    } catch (error) {
        if (error.response) {
            const { status, data } = error.response;

            if (status === 400) {
                console.error("Validation failed:");
                if (data.errors && Array.isArray(data.errors)) {
                    data.errors.forEach(err => {
                        console.error(`  ${err.field}: [${err.code}] ${err.message}`);
                    });
                }
            } else if (status === 409) {
                console.error("Conflict:", data.message);
            } else {
                console.error("Error:", data.message);
            }
        } else {
            console.error("Network error:", error.message);
        }
    }
}

// ============ VALIDATION HELPER ============

/**
 * Client-side validation rules that mirror server-side constraints
 * This helps catch errors before sending to server and provides instant feedback
 */
const clientValidationRules = {
    fullName: {
        required: true,
        minLength: 2,
        maxLength: 100,
        pattern: /^[a-zA-Z\s'-]+$/
    },
    email: {
        required: true,
        pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    },
    password: {
        required: true,
        minLength: 8,
        validate(value) {
            // Must have at least one uppercase letter and one digit
            const hasUpperCase = /[A-Z]/.test(value);
            const hasDigit = /\d/.test(value);
            return hasUpperCase && hasDigit;
        }
    },
    workspaceName: {
        required: true,
        minLength: 3,
        maxLength: 50
    },
    workspaceSlug: {
        required: false,
        minLength: 3,
        maxLength: 60,
        pattern: /^[a-z0-9]+(?:-[a-z0-9]+)*$/
    }
};

function validateBeforeSubmit(data) {
    const errors = {};

    for (const [field, value] of Object.entries(data)) {
        const rules = clientValidationRules[field];
        if (!rules) continue;

        // Check required
        if (rules.required && (!value || value.trim() === "")) {
            errors[field] = "This field is required";
            continue;
        }

        if (!value) continue; // Optional field is empty, so valid

        // Check min length
        if (rules.minLength && value.length < rules.minLength) {
            errors[field] = `Minimum ${rules.minLength} characters required`;
            continue;
        }

        // Check max length
        if (rules.maxLength && value.length > rules.maxLength) {
            errors[field] = `Maximum ${rules.maxLength} characters allowed`;
            continue;
        }

        // Check pattern
        if (rules.pattern && !rules.pattern.test(value)) {
            errors[field] = "Invalid format";
            continue;
        }

        // Custom validation function
        if (rules.validate && !rules.validate(value)) {
            errors[field] = "Validation failed";
            continue;
        }
    }

    return errors;
}

function submitFormWithValidation(jsonData) {
    const errors = validateBeforeSubmit(jsonData);

    if (Object.keys(errors).length > 0) {
        console.error("Client validation failed:", errors);
        return false;
    }

    // Client validation passed, safe to send to server
    return true;
}

// ============ FULL FORM SUBMISSION EXAMPLE ============

async function handleRegisterForm(event) {
    event.preventDefault();

    // Get form data
    const formElement = event.target;
    const formData = new FormData(formElement);
    const data = Object.fromEntries(formData);

    // Validate on client side first
    if (!submitFormWithValidation(data)) {
        console.error("Please fix validation errors");
        return;
    }

    // If client validation passes, send to server
    try {
        const response = await fetch(API_URL, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(data)
        });

        if (!response.ok) {
            const error = await response.json();

            if (response.status === 400 && error.errors) {
                // Server validation failed
                console.error("Server validation errors:");
                error.errors.forEach(fieldError => {
                    console.error(`  ${fieldError.field} [${fieldError.code}]: ${fieldError.message}`);
                });
            } else {
                console.error("Server error:", error.message);
            }
            return;
        }

        const result = await response.json();
        console.log("Success! User registered:", result);
        formElement.reset();
    } catch (err) {
        console.error("Network error:", err);
    }
}

// Usage:
// <form onsubmit="handleRegisterForm(event)">
//   <input type="text" name="fullName" required />
//   <input type="email" name="email" required />
//   <input type="password" name="password" required />
//   <input type="text" name="workspaceName" required />
//   <input type="text" name="workspaceSlug" />
//   <button type="submit">Register</button>
// </form>

