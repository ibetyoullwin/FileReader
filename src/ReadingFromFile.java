import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ReadingFromFile {
    private static Map<String, Set<String>> map = new HashMap<>(); //в чём соль выносить переменную, которая используется только в одном методе в поле?

    public static void main(String[] args) {
        Lock lock = new ReentrantLock();
        ExecutorService service = Executors.newCachedThreadPool();//почему именно этот пул?

        for (String fileName : args) {
            service.submit(() -> {
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new FileReader(fileName));
                } catch (FileNotFoundException e) {
                    System.out.println("Файл " + fileName + " не найден");
                }
                try {//Файл не найден, ошибку перехватили и всё равно пытаемся что-то начать парсить?
                    String[] titles = br.readLine().split(";");
                    for (int i = 0; i < titles.length; i++) {
                        lock.lock();//Зачем?
                        if (!map.containsKey(titles[i])) {
                            map.put(titles[i], new HashSet<>());
                        }
                        lock.unlock();
                    }
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] values = line.split(";");
                        for (int i = 0; i < titles.length; i++) {
                            lock.lock();//зачем?
                            Set<String> sValues = map.get(titles[i]);
                            sValues.add(values[i]);
                            map.put(titles[i], sValues);
                            lock.unlock();
                        }
                    }
                    br.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
        }
        service.shutdown();
        try {
            service.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(entry.getKey() + ".csv"));//try-with-resources?
                Set<String> sValues = entry.getValue();
                for (String str : sValues) {
                    bw.write(str + ";");
                }
                bw.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    //ООП? области видимости? Потокобезопасные коллекции? build-tools? log-frameworks? тесты?
}
