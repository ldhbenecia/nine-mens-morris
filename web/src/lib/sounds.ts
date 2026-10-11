import click from '/src/assets/sounds/click.mp3';
import join from '/src/assets/sounds/join.mp3';
import start from '/src/assets/sounds/start.mp3';
import whiteStone from '/src/assets/sounds/white_stone.mp3';
import blackStone from '/src/assets/sounds/black_stone.mp3';
import stoneDropping from '/src/assets/sounds/stone_dropping.mp3';
import explosion from '/src/assets/sounds/explosion.mp3';
import win from '/src/assets/sounds/win.mp3';
import loss from '/src/assets/sounds/loss.mp3';
import error from '/src/assets/sounds/error.mp3';
import notification from '/src/assets/sounds/notification.mp3';

// new Audio(src) 는 만드는 순간 파일을 받기 시작함
// 모듈에서 바로 만들면 메인 화면만 열어도 효과음 11개(약 680KB, 첫 접속의 80%)를 받음
// 처음 쓸 때 만들고, 게임 화면에서 쓰는 것은 그 화면에 들어올 때 미리 받음
class Sound {
  private readonly src: string;
  private audio: HTMLAudioElement | null = null;

  constructor(src: string) {
    this.src = src;
  }

  preload() {
    this.element();
  }

  // 항상 처음부터. 연달아 놓을 때 앞 소리가 끝나길 기다리지 않음
  play() {
    const audio = this.element();
    audio.currentTime = 0;
    // 화면을 한 번도 누르지 않았으면 브라우저가 자동 재생을 막음. 소리만 빠질 뿐 진행에는 지장 없음
    audio.play().catch(() => {});
  }

  private element() {
    this.audio ??= new Audio(this.src);
    return this.audio;
  }
}

export const clickSound = new Sound(click);
export const joinSound = new Sound(join);
export const startSound = new Sound(start);
export const whiteStoneSound = new Sound(whiteStone);
export const blackStoneSound = new Sound(blackStone);
export const stoneDroppingSound = new Sound(stoneDropping);
export const explosionSound = new Sound(explosion);
export const winSound = new Sound(win);
export const lossSound = new Sound(loss);
export const errorSound = new Sound(error);
export const notificationSound = new Sound(notification);

// 판 도중에 처음 받으면 착수 소리가 한 박자 늦음
export const preloadGameSounds = () =>
  [
    joinSound,
    startSound,
    whiteStoneSound,
    blackStoneSound,
    stoneDroppingSound,
    explosionSound,
    winSound,
    lossSound,
    errorSound,
    notificationSound,
  ].forEach((sound) => sound.preload());
