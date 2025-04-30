

# Centsible

Centsible is a personal finance management Android app that empowers you to track your income and expenses, manage your transactions, and safeguard your financial data. With features like data backup/restore, Excel export, daily reminders, and budget alerts, Centsible offers a comprehensive solution to stay in control of your finances.

## Features

- **Transaction Management**  
  Create, edit, and delete transactions. Each transaction stores details such as title, amount, date, category, and whether it’s income or an expense.

- **Backup & Restore**  
  Securely backup your transaction data as a JSON-formatted string stored in SharedPreferences and restore it whenever needed—ensuring that your data stays safe even if you switch devices.

- **Export to Excel**  
  Export your transactions to an Excel (XLSX) file, making it easy to perform offline analysis or share reports with others.

- **Currency Management**  
  Choose your preferred currency (e.g., LKR, USD, EUR, GBP, INR, JPY) for a personalized experience that fits your locale.

- **Daily Reminders & Budget Alerts**  
  Stay on track with your finances using daily reminders and real-time budget alerts. The app leverages Android’s WorkManager to schedule notifications and reminders.

- **User Feedback & Authentication**  
  Provide feedback directly via email and seamlessly log out to maintain personal data security.

## Technologies Used

- **Kotlin & Android SDK**  
  Built entirely in Kotlin using Android’s native APIs and libraries.

- **Material Design Components**  
  Implements modern Material Design to ensure a clean, intuitive, and responsive user interface.

- **View Binding**  
  Simplifies UI interactions and reduces boilerplate code by binding views directly in code.

- **SharedPreferences & Gson**  
  Persists user data and configurations using SharedPreferences with JSON serialization/deserialization via the Gson library.

- **Apache POI (with custom modifications)**  
  Generates Excel files (XLSX) for data export, with compatibility adjustments to avoid desktop-only dependencies.

- **WorkManager**  
  Schedules daily reminders and budget notifications reliably across device reboots.

- **RecyclerView**  
  Efficiently displays lists of transactions with smooth scrolling and minimal memory overhead.

## Getting Started

### Prerequisites
- Android Studio (Arctic Fox or later recommended)
- An Android device/emulator running Android 5.0 (API 21) or later

### Installation
1. **Clone the repository:**
   ```bash
   https://github.com/NPraveenLiyanage/Centsible.git
   ```
2. **Open the project in Android Studio.**
3. **Build the project** to download all required dependencies.
4. **Run the app** on your emulator or a physical device.

## Contributing

Contributions are welcome! If you have ideas, bug fixes, or improvements, please open an issue or submit a pull request. For major changes, please discuss them in an issue first.

## License

This project is licensed under the MIT License – see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Thanks to the Android team for continuous innovation and documentation.
- Appreciation to the open-source community for the excellent libraries like Apache POI and Gson that were used in this project.

---

*Centsible is designed to provide a simple yet powerful tool for managing your finances. Enjoy tracking your transactions and staying financially organized!*
