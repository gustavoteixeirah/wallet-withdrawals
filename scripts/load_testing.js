import http from 'k6/http';
import { check, sleep } from 'k6';

// Load testing options
export const options = {
    vus: 10, // 10 virtual users
    duration: '30s', // Run for 30 seconds
};

// Helper functions to generate random data
function randomInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

function randomString(length) {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz';
    let result = '';
    for (let i = 0; i < length; i++) {
        result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return result;
}

function randomNumberString(length) {
    const chars = '0123456789';
    let result = '';
    for (let i = 0; i < length; i++) {
        result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return result;
}

export default function () {
    const url = 'http://localhost:8080/api/v1/wallet_withdraw';
    const payload = JSON.stringify({
        userId: randomInt(1, 1000), // Random user ID between 1 and 1000
        amount: parseFloat((Math.random() * 1000).toFixed(2)), // Random amount between 0.00 and 1000.00
        recipientFirstName: randomString(8), // Random 8-char first name
        recipientLastName: randomString(10), // Random 10-char last name
        recipientRoutingNumber: randomNumberString(9), // Random 9-digit routing number
        recipientNationalId: randomNumberString(9), // Random 9-digit national ID
        recipientAccountNumber: randomNumberString(9), // Random 9-digit account number
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    const res = http.post(url, payload, params);

    check(res, {
        'is status 200': (r) => r.status === 200,
        'response time < 50ms': (r) => r.timings.duration < 500,
    });

    sleep(1); // Pause between requests
}