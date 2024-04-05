import { Point } from './Point';

export function Board() {
  return (
    <div className="relative">
      <div className="absolute left-[-40px] top-[-40px] z-10 grid h-[560px] w-[560px] grid-cols-7 grid-rows-7 place-items-center">
        <Point />
        <Point unused />
        <Point unused />
        <Point stone="WHITE" />
        <Point unused />
        <Point unused />
        <Point stone="BLACK" selected />

        <Point />
        <Point unused />
        <Point unused />
        <Point stone="WHITE" />
        <Point unused />
        <Point unused />
        <Point stone="BLACK" selected />

        <Point />
        <Point unused />
        <Point unused />
        <Point stone="WHITE" />
        <Point unused />
        <Point unused />
        <Point stone="BLACK" />

        <Point />
        <Point unused />
        <Point unused />
        <Point unused />
        <Point unused />
        <Point unused />
        <Point stone="BLACK" selected />

        <Point />
        <Point stone="WHITE" unused />
        <Point stone="WHITE" />
        <Point stone="WHITE" selected />
        <Point unused />
        <Point unused />
        <Point stone="BLACK" selected />

        <Point />
        <Point unused />
        <Point unused />
        <Point stone="WHITE" />
        <Point unused />
        <Point unused />
        <Point stone="BLACK" selected />

        <Point />
        <Point unused />
        <Point unused />
        <Point stone="WHITE" />
        <Point unused />
        <Point unused />
        <Point stone="BLACK" selected />
      </div>
      <div className="relative h-[480px]  w-[480px]">
        <div className="flex h-[480px] w-[480px] items-center justify-center border border-gray-400">
          <div className="flex h-[320px] w-[320px] items-center justify-center border border-gray-400">
            <div className="h-[160px] w-[160px] border border-gray-400" />
          </div>
        </div>
        <div className="absolute left-[240px] top-0 h-[160px] border-l border-gray-400" />
        <div className="absolute bottom-0 left-[240px] h-[160px] border-l border-gray-400" />
        <div className="absolute left-0 top-[240px] w-[160px] border-t border-gray-400" />
        <div className="absolute right-0 top-[240px] w-[160px] border-t border-gray-400" />
      </div>
    </div>
  );
}
