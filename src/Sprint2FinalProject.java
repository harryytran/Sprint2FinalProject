// Import necessary libraries
import java.math.BigDecimal; // For handling monetary values
import java.util.Stack; // For managing the currency path
import org.json.JSONObject; // For parsing JSON data
import java.io.BufferedReader; // For reading input streams
import java.io.IOException; // For handling IO exceptions
import java.io.InputStreamReader; // For reading input streams
import java.net.HttpURLConnection; // For making HTTP connections
import java.net.URL; // For URL handling
import java.util.Arrays; // For array manipulation

// Interface representing a rate
interface Rate {
    BigDecimal getRate(); // Method to get the rate as BigDecimal
}

// Implementation of Rate using BigDecimal
class BigDecimalRate implements Rate {
    private BigDecimal rate; // Holds the rate value

    public BigDecimalRate(BigDecimal rate) {
        this.rate = rate; // Constructor to initialize the rate
    }

    public BigDecimal getRate() {
        return rate; // Returns the rate
    }
}

// Implementation of Rate using Number
class NumberRate implements Rate {
    private Number rate; // Holds the rate value

    public NumberRate(Number rate) {
        this.rate = rate; // Constructor to initialize the rate
    }

    public BigDecimal getRate() {
        return BigDecimal.valueOf(rate.doubleValue()); // Converts Number to BigDecimal
    }
}

// Class representing a currency path
class CurrencyPath {
    private Stack<String> path; // Stack to hold the currency path
    private BigDecimal runningTotal; // Holds the running total of the conversion
    private Object[][] allRates; // 2D array to hold all currency rates
    private String[] listOfCurrencies; // Array of currency codes

    public CurrencyPath(Object[][] allRates, String[] listOfCurrencies) {
        this.path = new Stack<>(); // Initialize the path stack
        this.runningTotal = BigDecimal.ONE; // Start with a total of 1
        this.allRates = allRates; // Store the rates
        this.listOfCurrencies = listOfCurrencies; // Store the list of currencies
    }

    // Find the optimal path based on given parameters
    public String[] findOptimal(int depth, String start) {
        recurse(depth, start); // Start the recursive search
        return path.toArray(new String[0]); // Return the path as an array
    }

    // Getter for the path
    public Stack<String> getPath() {
        return path; // Returns the current path
    }

    // Getter for the list of currencies
    public String[] getListOfCurrencies() {
        return listOfCurrencies; // Returns the list of currencies
    }

    // Getter for all rates
    public Object[][] getAllRates() {
        return allRates; // Returns the rates
    }

    // Recursive function to find the optimal path
    private void recurse(int depth, String start) {
        if (depth == 0) {
            // Base case: reached the desired depth
            path.push(start); // Push the current currency onto the path
            return; // Exit the recursion
        }

        int startI = Arrays.asList(listOfCurrencies).indexOf(start); // Find index of the starting currency
        BigDecimal bestValue = BigDecimal.ZERO; // Initialize best value
        String bestNextCurrency = ""; // Initialize best next currency

        // Loop through all currencies to find the best next step
        for (int i = 0; i < listOfCurrencies.length; i++) {
            if (i != startI) { // Avoid the starting currency
                Rate forwardRate = createRate(allRates[startI][i]); // Get forward rate
                Rate backwardRate = createRate(allRates[i][startI]); // Get backward rate

                BigDecimal forwardValue = runningTotal.multiply(forwardRate.getRate()); // Calculate forward value
                BigDecimal backwardValue = forwardValue.multiply(backwardRate.getRate()); // Calculate backward value

                // Check if the backward value is better than the best found so far
                if (backwardValue.compareTo(bestValue) > 0) {
                    bestValue = backwardValue; // Update best value
                    bestNextCurrency = listOfCurrencies[i]; // Update best next currency
                }
            }
        }

        // Update the path and running total
        path.push(start); // Push the current currency onto the path
        runningTotal = bestValue; // Update the running total

        // Recursive call with updated values
        recurse(depth - 1, bestNextCurrency); // Continue to the next currency
    }

    // Factory method to create a Rate based on the rateObject
    public Rate createRate(Object rateObject) {
        if (rateObject instanceof BigDecimal) {
            return new BigDecimalRate((BigDecimal) rateObject); // Create BigDecimalRate
        } else if (rateObject instanceof Number) {
            return new NumberRate((Number) rateObject); // Create NumberRate
        } else {
            // Handle other cases if needed
            throw new IllegalArgumentException("Unsupported rate type"); // Throw exception for unsupported types
        }
    }
}

// Main class for the project
public class Sprint2FinalProject {

    // Calculate and print the result based on the optimal path
    public static void calculateResult(CurrencyPath currencyPath, String start, int depth) {
        currencyPath.findOptimal(depth, start); // Find the optimal path
        System.out.println("Path: (from 1.00 of starting currency)"); // Print path header
        Stack<String> path = currencyPath.getPath(); // Get the path
        BigDecimal result = BigDecimal.ONE; // Initialize result

        String currentCurrency = start; // Start with the initial currency
        while (!path.isEmpty()) { // While there are currencies in the path
            String nextCurrency = path.pop(); // Get the next currency
            int currentIndex = Arrays.asList(currencyPath.getListOfCurrencies()).indexOf(currentCurrency); // Get current index
            int nextIndex = Arrays.asList(currencyPath.getListOfCurrencies()).indexOf(nextCurrency); // Get next index

            Rate rate = currencyPath.createRate(currencyPath.getAllRates()[currentIndex][nextIndex]); // Get the rate
            BigDecimal rateValue = rate.getRate(); // Get the rate value

            System.out.println(currentCurrency + " -> " + nextCurrency + " : " + rateValue); // Print conversion
            result = result.multiply(rateValue); // Update the result
            currentCurrency = nextCurrency; // Move to the next currency
        }

        // Connect the last currency back to the starting one
        int lastIndex = Arrays.asList(currencyPath.getListOfCurrencies()).indexOf(currentCurrency); // Get last index
        int startIndex = Arrays.asList(currencyPath.getListOfCurrencies()).indexOf(start); // Get start index

        Rate lastRate = currencyPath.createRate(currencyPath.getAllRates()[lastIndex][startIndex]); // Get last rate
        BigDecimal lastRateValue = lastRate.getRate(); // Get last rate value

        // System.out.println(currentCurrency + " -> " + start + " : " + lastRateValue); // Print last conversion
        result = result.multiply(lastRateValue); // Update result with last rate
        result = result.setScale(4, BigDecimal.ROUND_HALF_UP); // Set scale for result
        System.out.println("\nResult: " + result); // Print final result
    }

    // Connect to the API and retrieve the initial data
    public static JSONObject connect(String apiAddress) {
        try {
            // Create URL object with the API address
            URL url = new URL(apiAddress);

            // Open a connection to the URL
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set the request method to GET
            connection.setRequestMethod("GET");

            // Set the timeout for the connection
            connection.setConnectTimeout(5000); // 5 seconds

            // Check if the request was successful (HTTP status 200)
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                // Read the response
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder response = new StringBuilder();

                while ((line = reader.readLine()) != null) {
                    response.append(line); // Append each line to the response
                }
                reader.close(); // Close the reader

                // Parse the JSON response
                JSONObject jsonResponseObject = new JSONObject(response.toString());
                return jsonResponseObject; // Return the parsed JSON object
            } else {
                // Handle error response
                System.out.println("Error: " + connection.getResponseCode() + " " + connection.getResponseMessage()); // Print error
            }
            // Close the connection
            connection.disconnect();

        } catch (IOException e) {
            e.printStackTrace(); // Print stack trace for exceptions
        }
        return null; // Return null if connection fails
    }

    // Main method to run the project
    public static void main(String[] args) {
        // API Address (example)
        String apiAddress = "https://open.er-api.com/v6/latest/USD"; // API endpoint for USD rates

        JSONObject jsonResponseObject = connect(apiAddress); // Connect to the API and get response

        // Extract information from the JSON
        JSONObject rates = jsonResponseObject.getJSONObject("rates"); // Get rates object from JSON
        String[] keys = new String[rates.keySet().size()]; // Create array for currency keys
        keys = rates.keySet().toArray(keys); // Populate array with keys

        Object[][] allRates = new Object[keys.length][keys.length]; // Create 2D array for all rates
        for (int i = 0; i < keys.length; i++) {
            String apiString = "https://open.er-api.com/v6/latest/" + keys[i]; // Create API string for each currency
            JSONObject response = connect(apiString).getJSONObject("rates"); // Get rates for each currency
            for (int j = 0; j < keys.length; j++) {
                allRates[i][j] = response.get(keys[j]); // Populate rates array
            }
        }

        String start = "USD"; // Set starting currency
        int depth = 1000; // Set depth for recursion
        CurrencyPath currencyPath = new CurrencyPath(allRates, keys); // Create CurrencyPath object
        calculateResult(currencyPath, start, depth); // Calculate and print the result
    }
}