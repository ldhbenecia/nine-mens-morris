import React from 'react';
import { Button } from './../../components/Button';

export function Home() {
  return (
    <main className="flex h-full flex-col items-center justify-center gap-12">
      <div className="flex flex-col items-center gap-2">
        <h1 className="font-title text-4xl">Nine Men&apos;s Morris</h1>
        <h2 className="text-xl font-light tracking-[0.8rem] text-gray-500">
          나인 멘스 모리스
        </h2>
      </div>
      <div className="flex w-60 flex-col gap-4">
        <Button text="게임 시작" onClick={() => {}} />
        <Button theme="secondary" text="랭킹 보기" onClick={() => {}} />
      </div>
    </main>
  );
}
