/// <reference types="vite/client" />
/// <reference types="vite-plugin-svgr/client" />

// 선언이 없으면 import.meta.env 가 any 가 되어 오타나 누락을 못 잡는다
// VITE_SOCKET_URL 이 비면 brokerURL 이 undefined 인 채로 무한 재시도만 한다
interface ImportMetaEnv {
  readonly VITE_API_URL: string;
  readonly VITE_SOCKET_URL: string;
  readonly VITE_KAKAO_URL: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
