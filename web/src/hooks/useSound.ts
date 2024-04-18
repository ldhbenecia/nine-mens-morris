import { Howl } from 'howler';
import click from '~/assets/sounds/click.mp3';
import join from '~/assets/sounds/join.mp3';
import phase from '~/assets/sounds/phase.mp3';
import whiteStone from '~/assets/sounds/white_stone.mp3';
import blackStone from '~/assets/sounds/black_stone.mp3';
import stoneDropping from '~/assets/sounds/stone_dropping.mp3';
import explosion from '~/assets/sounds/explosion.mp3';
import win from '~/assets/sounds/win.mp3';
import loss from '~/assets/sounds/loss.mp3';

export function useSound() {
  const clickSound = new Howl({ src: click });
  const joinSound = new Howl({ src: join });
  const phaseSound = new Howl({ src: phase });
  const whiteStoneSound = new Howl({ src: whiteStone });
  const blackStoneSound = new Howl({ src: blackStone });
  const stoneDroppingSound = new Howl({ src: stoneDropping });
  const explosionSound = new Howl({ src: explosion });
  const winSound = new Howl({ src: win });
  const lossSound = new Howl({ src: loss });

  return {
    clickSound,
    joinSound,
    phaseSound,
    whiteStoneSound,
    blackStoneSound,
    stoneDroppingSound,
    explosionSound,
    winSound,
    lossSound,
  };
}
