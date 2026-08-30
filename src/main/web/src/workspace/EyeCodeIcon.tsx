type Props = { name: string; label?: string; className?: string };

export function EyeCodeIcon({ name, label, className = '' }: Props) {
  return <img className={`eyecode-icon ${className}`} src={`icons/${name}.svg`} alt={label ?? ''} aria-hidden={label ? undefined : true} />;
}
