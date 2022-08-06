import { useState } from 'react';
import { AppControllerApi, Configuration } from './generated';

const api = new AppControllerApi(new Configuration({ basePath: window.location.origin }));

export default function App() {

  const [arg1, setArg1] = useState<string>("");
  const [arg2, setArg2] = useState<string>("");
  const [result, setResult] = useState<number>();

  function add() {
    if (!arg1 || !arg2) {
      return;
    }
    api.add({ arg1: Number(arg1), arg2: Number(arg2) }).then(response => {
      setResult(response.result);
    });
  };

  return (
    <>
      <input type="text" value={arg1} onChange={(e) => setArg1(e.target.value)} autoFocus />
      +
      <input type="text" value={arg2} onChange={(e) => setArg2(e.target.value)} />
      <input type="button" value=" = " onClick={add} />
      <span>{result}</span>
    </>
  );
}

