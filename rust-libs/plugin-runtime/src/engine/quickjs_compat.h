#ifndef TT_QUICKJS_COMPAT_H
#define TT_QUICKJS_COMPAT_H
#include <stddef.h>
#include <stdint.h>
typedef struct TTQuickJs TTQuickJs;
typedef struct { const char *data; size_t length; } TTQjsStringView;
typedef int (*TTQjsHostCallFn)(void *, TTQjsStringView, TTQjsStringView, TTQjsStringView *, TTQjsStringView *);
typedef int (*TTQjsShouldInterruptFn)(void *);
typedef struct { uint64_t memory_limit_bytes; uint64_t stack_limit_bytes; TTQjsHostCallFn host_call; TTQjsShouldInterruptFn should_interrupt; void *opaque; } TTQjsOptions;
TTQuickJs *tt_qjs_create(const TTQjsOptions *options, char **error);
int tt_qjs_eval(TTQuickJs *, const char *, size_t, const char *, char **, char **);
int tt_qjs_call_json(TTQuickJs *, const char *, const char *, size_t, char **, char **);
void tt_qjs_free_string(char *value);
void tt_qjs_destroy(TTQuickJs *runtime);
#endif
