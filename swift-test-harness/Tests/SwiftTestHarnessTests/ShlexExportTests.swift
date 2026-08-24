import Testing
import Shlex

@Suite("Shlex Swift Export Tests")
struct ShlexExportTests {
    @Test("Swift module loads")
    func swiftModuleLoads() {
        #expect(true, "Shlex swift module imported cleanly")
    }
}
