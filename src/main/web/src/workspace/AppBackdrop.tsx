import { motion, type MotionValue } from 'motion/react';
import glowBottom from './assets/app-glow-bottom.svg';
import glowTop from './assets/app-glow-top.svg';
import noiseTile from './assets/app-noise-300.png';

type Props = {
  scale?: MotionValue<number> | null;
};

export function AppBackdrop({ scale }: Props) {
  return <div className="app-background" aria-hidden="true">
    <motion.div className="app-backdrop-glow is-top" style={scale ? { scale } : undefined}>
      <img src={glowTop} alt="" />
    </motion.div>
    <div className="app-backdrop-glow is-bottom">
      <img src={glowBottom} alt="" />
    </div>
    <div className="app-backdrop-noise" style={{ backgroundImage: `url(${noiseTile})` }} />
  </div>;
}
