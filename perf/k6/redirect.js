import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: Number(__ENV.VUS || 20),
  duration: __ENV.DURATION || '1m',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<200'],
  },
};

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const LINK_ID = __ENV.LINK_ID || '1';
const ALIAS = __ENV.ALIAS || '';

export default function () {
  const path = ALIAS ? `/r/a/${ALIAS}` : `/r/${LINK_ID}`;
  const res = http.get(`${BASE}${path}`, { redirects: 0 });
  check(res, {
    '302 redirect': (r) => r.status === 302,
    'has Location': (r) => !!r.headers['Location'],
  });
  sleep(0.1);
}


