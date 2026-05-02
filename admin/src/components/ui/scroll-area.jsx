import React from 'react';

export function ScrollArea({ className = '', children, ...props }) {
  const classes = ['scroll-area-root', className].filter(Boolean).join(' ');

  return (
    <div className={classes} {...props}>
      <div className="scroll-area-viewport">{children}</div>
    </div>
  );
}
