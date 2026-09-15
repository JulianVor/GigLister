/** Small inline bar + percentage showing how much of an entity's optional profile data
 * (see AdminService.bandCompleteness/locationCompleteness/eventCompleteness) is filled in. */
export function CompletenessBadge({ percent }: { percent: number }) {
  return (
    <div className="flex items-center gap-1.5" title={`${percent}% vollständig`}>
      <div className="h-1.5 w-12 overflow-hidden bg-line">
        <div className="h-full bg-accent" style={{ width: `${percent}%` }} />
      </div>
      <span className="w-8 shrink-0 font-meta text-xs text-muted">{percent}%</span>
    </div>
  );
}
