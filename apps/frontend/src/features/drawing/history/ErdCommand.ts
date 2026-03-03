export interface ErdCommand {
  undo(): Promise<void>;
  redo(): Promise<void>;
  merge?(other: ErdCommand): ErdCommand | null;
}
