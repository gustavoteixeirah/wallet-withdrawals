import http from 'k6/http';
import { check, sleep } from 'k6';

// Load testing options with ramp-up stages and thresholds
export const options = {
    stages: [
        { duration: '10s', target: 1000 },  // Ramp up to 10 VUs (warm-up)
        { duration: '20s', target: 5000 },  // Hold at 50 VUs
        { duration: '20s', target: 10000 }, // Ramp to 100 VUs
        { duration: '20s', target: 20000 }, // Ramp to 200 VUs (increase this in later runs if no failure)
        { duration: '10s', target: 0 },   // Ramp down
    ],
    thresholds: {
        http_req_failed: ['rate<0.01'],          // Error rate must be <1%
        http_req_duration: ['p(95)<500'],        // 95% of requests <500ms
        checks: ['rate>0.99'],                   // 99% of checks must pass
    },
};

// Helper functions for random data (unchanged from your script)
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
        userId: randomInt(1, 1000),
        amount: parseFloat((Math.random() * 1000).toFixed(2)),
        recipientFirstName: randomString(8),
        recipientLastName: randomString(10),
        recipientRoutingNumber: randomNumberString(9),
        recipientNationalId: randomNumberString(9),
        recipientAccountNumber: randomNumberString(9),
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    const res = http.post(url, payload, params);

    check(res, {
        'is status 200': (r) => r.status === 200,
        'response time < 500ms': (r) => r.timings.duration < 500,
    });

    sleep(1); // Adjust or remove for higher load
}