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

// 같은 브라우저의 다른 탭에서 로그인·로그아웃·게스트 시작을 하면 토큰이 바뀜
// 이 탭은 열어 둔 소켓은 예전 신원, 다시 받은 내 정보는 새 신원이 되어 내 돌을 상대 돌로 보게 됨
// 다시 읽어 한 신원으로 맞춤. storage 이벤트는 바꾼 탭이 아니라 다른 탭에만 옴
export const reloadWhenTokenChangesElsewhere = () => {
  window.addEventListener('storage', (event) => {
    // key 가 null 이면 저장소 전체를 지운 것
    if (event.key === TOKEN_KEY || event.key === null) {
      window.location.reload();
    }
  });
};

export const clearToken = () => {
  try {
    localStorage.removeItem(TOKEN_KEY);
  } catch {
    // 지울 수 없어도 할 수 있는 게 없음
  }
};
