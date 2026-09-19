const TOKEN_KEY = 'access_token';

// 토큰을 쿠키가 아니라 여기에 둔다
// 프론트가 GitHub Pages 라 서버와 cross-site 이고, 그러면 쿠키는
// SameSite=None; Secure 여야만 실리는데 그건 CSRF 방어를 스스로 버리는 것이다
// 헤더로 보내면 브라우저가 자동으로 붙이지 않으므로 CSRF 자체가 성립하지 않는다
//
// 대신 XSS 가 나면 토큰을 읽힌다. 유효기간을 짧게 두는 것으로 감수함
export const getToken = () => {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
};

export const setToken = (token: string) => {
  try {
    localStorage.setItem(TOKEN_KEY, token);
  } catch {
    // 시크릿 모드나 저장 차단. 이 탭에서만 못 쓰고 끝남
  }
};

// STOMP CONNECT 프레임에 실을 헤더
// 토큰이 없으면 빈 값을 보내고 서버가 연결을 거절하게 둔다
export const authHeaders = (): Record<string, string> => {
  const token = getToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
};

export const clearToken = () => {
  try {
    localStorage.removeItem(TOKEN_KEY);
  } catch {
    // 지울 수 없어도 할 수 있는 게 없음
  }
};
