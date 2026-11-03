/** Strategy used by a building to decide who may use its available places. */
public interface AdmissionPolicy {
    boolean allows(Human person, Building building);
    String describe();
}