package cyberpunk_decryption;

public class Main {

  public static void main(String[] args) {
    String[] data = {
        "1c 1c 1c ff bd e9 e9",
        "55 bd bd e9 1c bd bd",
        "55 1c 1c bd e9 e9 1c",
        "55 7a 1c ff 1c 1c 7a",
        "1c 7a bd e9 66 bd 55",
        "e9 e9 55 7a bd e9 ff",
        "1c 1c 7a 1c 1c e9 e9"
    };
    String[] sequences = {
        "e9 1c",
        "55 bd e9",
        "1c bd e9"
    };

    Solver solver = new Solver(data, sequences, 5);
    solver.solve();
    solver.print();
  }

}
