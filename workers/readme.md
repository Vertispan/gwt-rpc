Even more than the rest of this project, this module tends toward bleeding edge. Dev mode is simply not supported,
and typed arrays are required to be fully functioning. It wouldn't be impossible to add dev mode support, but it
might not be worth the time it would take to branch in cases where TypedArrays as not actually JSOs.

Tests will appear to pass in dev mode, since the tests are all marked as `@DoNotRunWith(Platform.Devel)`.