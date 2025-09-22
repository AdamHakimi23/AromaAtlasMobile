// File: script/auth-check.js

// Firebase imports
import { initializeApp } from "https://www.gstatic.com/firebasejs/11.9.1/firebase-app.js";
import {
  getAuth,
  onAuthStateChanged
} from "https://www.gstatic.com/firebasejs/11.9.1/firebase-auth.js";
import {
  getFirestore,
  doc,
  getDoc
} from "https://www.gstatic.com/firebasejs/11.9.1/firebase-firestore.js";

// Firebase config
const firebaseConfig = {
  apiKey: "AIzaSyD6NAG1a8Bv8SiNBPBcZBWdpldK7Rgo0gg",
  authDomain: "aromaatlas-ff5cd.firebaseapp.com",
  projectId: "aromaatlas-ff5cd",
  storageBucket: "aromaatlas-ff5cd.appspot.com", // Fixed: should be .appspot.com not .app
  messagingSenderId: "1084697066953",
  appId: "1:1084697066953:web:9ae27d03d54025fbba7c18",
  measurementId: "G-96QJG9WKWY"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getFirestore(app);

// Check if the user is authenticated and has the "admin" role
onAuthStateChanged(auth, async (user) => {
  if (!user) {
    // User not logged in
    window.location.href = "login.html";
    return;
  }

  try {
    const userRef = doc(db, "users", user.uid);
    const userSnap = await getDoc(userRef);

    if (!userSnap.exists()) {
      throw new Error("User document does not exist.");
    }

    const role = userSnap.data().role;

    if (role !== "admin") {
      alert("Access denied. Admins only.");
      window.location.href = "login.html";
    }
  } catch (error) {
    console.error("Error checking user role:", error);
    window.location.href = "login.html";
  }
});
